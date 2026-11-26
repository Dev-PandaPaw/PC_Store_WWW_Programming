package iuh.fit.nhom16.pcstore.identity.application;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final Environment environment;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Autowired
    public EmailService(@Autowired(required = false) JavaMailSender mailSender,
                        @Autowired(required = false) Environment environment) {
        this.mailSender = mailSender;
        this.environment = environment;
    }

    public EmailService(JavaMailSender mailSender) {
        this(mailSender, null);
    }

    public void sendOtpEmail(String toEmail, String otpCode) {
        String subject = "[PixelCore PC Store] Mã xác thực OTP đặt lại mật khẩu";
        String intro = "Chào bạn, chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.";
        dispatchOtpEmail(toEmail, subject, intro, otpCode, 10);
    }

    public void sendPhoneVerificationOtpEmail(String toEmail, String phone, String otpCode) {
        String subject = "[PixelCore PC Store] Mã OTP xác minh số điện thoại";
        String intro = "Chào bạn, đây là mã xác minh cho số điện thoại nhận hàng " + phone + " trên tài khoản của bạn.";
        dispatchOtpEmail(toEmail, subject, intro, otpCode, 5);
    }

    private void dispatchOtpEmail(String toEmail, String subject, String intro, String otpCode, int validMinutes) {
        boolean isLocalDemo = environment == null || environment.acceptsProfiles(Profiles.of("local", "default", "test", "order-fixtures"));

        if (isLocalDemo && (mailSender == null || fromEmail == null || fromEmail.isBlank())) {
            log.info("[LOCAL DEMO OTP] Đích: {} | Hiệu lực: {} phút | Mã giả lập môi trường dev: {}", toEmail, validMinutes, otpCode);
            return;
        }

        if (mailSender == null || fromEmail == null || fromEmail.isBlank()) {
            log.warn("Chưa cấu hình SMTP spring.mail.username để gửi email xác thực tới {}", toEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "PixelCore PC Store");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(buildOtpHtmlContent(intro, otpCode, validMinutes), true);

            mailSender.send(message);
            log.info("Đã gửi email xác thực thành công đến: {}", toEmail);
        } catch (Exception e) {
            log.error("Không thể gửi email qua SMTP tới {}: {}", toEmail, e.getMessage());
            if (isLocalDemo) {
                log.info("[LOCAL DEMO OTP - SMTP FALLBACK] Đích: {} | Mã giả lập môi trường dev: {}", toEmail, otpCode);
            }
        }
    }

    private String buildOtpHtmlContent(String intro, String otpCode, int validMinutes) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px; color: #1e293b; }
                    .container { max-width: 520px; margin: 0 auto; background: #ffffff; border-radius: 12px; border: 1px solid #e2e8f0; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05); }
                    .header { background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%); padding: 24px; text-align: center; color: #ffffff; }
                    .header h1 { margin: 0; font-size: 22px; font-weight: 700; letter-spacing: 0.5px; }
                    .content { padding: 32px 24px; text-align: center; }
                    .greeting { font-size: 16px; margin-bottom: 16px; color: #334155; }
                    .otp-box { background: #f1f5f9; border: 2px dashed #0284c7; border-radius: 8px; padding: 18px 24px; margin: 24px auto; display: inline-block; }
                    .otp-code { font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #0284c7; font-family: monospace; }
                    .warning { font-size: 13px; color: #64748b; margin-top: 20px; line-height: 1.5; }
                    .footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 16px; text-align: center; font-size: 12px; color: #94a3b8; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>PIXELCORE PC STORE</h1>
                    </div>
                    <div class="content">
                        <div class="greeting">%s</div>
                        <p style="color: #64748b; font-size: 14px; margin: 0;">Mã xác thực OTP của bạn là:</p>
                        <div class="otp-box">
                            <span class="otp-code">%s</span>
                        </div>
                        <div class="warning">
                            Mã OTP này có hiệu lực trong vòng <strong>%d phút</strong>.<br>
                            Vì lý do an toàn, vui lòng <strong>không chia sẻ mã này</strong> cho bất kỳ ai khác.<br>
                            Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.
                        </div>
                    </div>
                    <div class="footer">
                        &copy; 2026 PixelCore PC Store. Hệ thống phân phối máy tính & linh kiện chính hãng.
                    </div>
                </div>
            </body>
            </html>
            """.formatted(intro, otpCode, validMinutes);
    }
}
