package com.example.campusease.service;

import com.example.campusease.dto.CheckoutResponse;
import com.example.campusease.dto.PaymentVerifyRequest;
import com.example.campusease.exception.BadRequestException;
import com.example.campusease.exception.ResourceNotFoundException;
import com.example.campusease.model.Order;
import com.example.campusease.model.Payment;
import com.example.campusease.model.PaymentStatus;
import com.example.campusease.repository.OrderRepository;
import com.example.campusease.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class PaymentService {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CartService cartService;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository, CartService cartService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.cartService = cartService;
    }

    @Transactional
    public CheckoutResponse createRazorpayOrder(Order order) {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new BadRequestException(
                "Razorpay is not configured on the server. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            // Razorpay expects the amount in the smallest currency unit (paise for INR)
            long amountInPaise = Math.round(order.getTotalAmount() * 100);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "campusease_order_" + order.getId());

            com.razorpay.Order rpOrder = client.orders.create(orderRequest);

            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setRazorpayOrderId(rpOrder.get("id"));
            payment.setAmount(order.getTotalAmount());
            payment.setStatus(PaymentStatus.CREATED);
            paymentRepository.save(payment);

            return new CheckoutResponse(order.getId(), rpOrder.get("id"), order.getTotalAmount(), "INR", keyId);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to create Razorpay order: " + e.getMessage());
        }
    }

        @Transactional
    public void refundOrder(Order order) {
        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No payment record found for order " + order.getId()));

        if (payment.getRazorpayPaymentId() == null || payment.getRazorpayPaymentId().isBlank()) {
            throw new BadRequestException("This order has no captured payment to refund.");
        }

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", Math.round(payment.getAmount() * 100));
            client.payments.refund(payment.getRazorpayPaymentId(), refundRequest);
        } catch (Exception e) {
            throw new BadRequestException("Refund failed: " + e.getMessage());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
    }

    @Transactional
    public Order verifyAndComplete(PaymentVerifyRequest request) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for this order"));

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean valid = Utils.verifyPaymentSignature(options, keySecret);
            if (!valid) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                throw new BadRequestException("Payment signature verification failed");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new BadRequestException("Payment verification error: " + e.getMessage());
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaymentTime(LocalDateTime.now());
        paymentRepository.save(payment);

        Order order = orderRepository.findById(request.getOrderId())
        .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentId(request.getRazorpayPaymentId());
        orderRepository.save(order);

        cartService.clearCart(order.getUser());

        return order;
    }
}
