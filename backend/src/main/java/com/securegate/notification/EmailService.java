package com.securegate.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String appBaseUrl;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${securegate.mail.from:noreply@securegate.local}") String fromEmail,
            @Value("${securegate.app-base-url:http://localhost:3000}") String appBaseUrl) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.appBaseUrl = appBaseUrl;
    }

    public void sendVerificationEmail(String toEmail, String token) {
        String verificationUrl = appBaseUrl + "/verify-email?token=" + token;
        String subject = "Verify your SecureGate account";
        String htmlContent = """
            <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; rounded: 8px;">
                <h2 style="color: #0284c7;">Welcome to SecureGate</h2>
                <p>Please verify your email address to complete your registration and activate your organization access.</p>
                <div style="margin: 24px 0;">
                    <a href="%s" style="background-color: #0284c7; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">
                        Verify Email Address
                    </a>
                </div>
                <p style="color: #64748b; font-size: 14px;">If the button above does not work, paste this link into your browser:<br/><a href="%s">%s</a></p>
                <p style="color: #94a3b8; font-size: 12px; margin-top: 32px;">This link expires in 24 hours. If you did not create an account, you can safely ignore this email.</p>
            </div>
            """.formatted(verificationUrl, verificationUrl, verificationUrl);

        sendMimeEmail(toEmail, subject, htmlContent);
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetUrl = appBaseUrl + "/reset-password?token=" + token;
        String subject = "SecureGate Password Reset Request";
        String htmlContent = """
            <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; rounded: 8px;">
                <h2 style="color: #0284c7;">Password Reset Request</h2>
                <p>We received a request to reset your password. Click the button below to choose a new secure password:</p>
                <div style="margin: 24px 0;">
                    <a href="%s" style="background-color: #0284c7; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">
                        Reset Password
                    </a>
                </div>
                <p style="color: #64748b; font-size: 14px;">This link will expire in 15 minutes. If you did not request a password reset, please contact your organization administrator immediately.</p>
            </div>
            """.formatted(resetUrl);

        sendMimeEmail(toEmail, subject, htmlContent);
    }

    public void sendMagicLinkEmail(String toEmail, String token) {
        String magicLinkUrl = appBaseUrl + "/verify-magic-link?token=" + token;
        String subject = "Your SecureGate Magic Sign-In Link";
        String htmlContent = """
            <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; rounded: 8px;">
                <h2 style="color: #0284c7;">Sign in to SecureGate</h2>
                <p>Click the secure button below to sign in directly without a password:</p>
                <div style="margin: 24px 0;">
                    <a href="%s" style="background-color: #0284c7; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">
                        Sign In Now
                    </a>
                </div>
                <p style="color: #64748b; font-size: 14px;">This single-use link is valid for 15 minutes.</p>
            </div>
            """.formatted(magicLinkUrl);

        sendMimeEmail(toEmail, subject, htmlContent);
    }

    public void sendInvitationEmail(String toEmail, String organizationName, String token, String roleName) {
        String inviteUrl = appBaseUrl + "/accept-invitation?token=" + token;
        String subject = "You have been invited to join " + organizationName + " on SecureGate";
        String htmlContent = """
            <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; rounded: 8px;">
                <h2 style="color: #0284c7;">Invitation to Join %s</h2>
                <p>You have been invited to join <strong>%s</strong> as a <strong>%s</strong>.</p>
                <div style="margin: 24px 0;">
                    <a href="%s" style="background-color: #0284c7; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">
                        Accept Invitation
                    </a>
                </div>
                <p style="color: #64748b; font-size: 14px;">This invitation expires in 7 days.</p>
            </div>
            """.formatted(organizationName, organizationName, roleName, inviteUrl);

        sendMimeEmail(toEmail, subject, htmlContent);
    }

    private void sendMimeEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Sent email subject='{}' to recipient='{}'", subject, to);
        } catch (Exception e) {
            log.warn("Failed to deliver email to {}: {}", to, e.getMessage());
            // Do not fail user requests if mail server is temporarily unavailable in development
        }
    }
}
