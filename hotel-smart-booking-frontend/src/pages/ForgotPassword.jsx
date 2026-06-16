import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import OtpInput from '../components/OtpInput';
import { forgotPassword, verifyForgotOTP, resetPassword } from '../services/authService';
import { useToast, ToastContainer } from '../components/Toast';

const emailSchema = z.object({
  email: z
    .string()
    .min(1, { message: 'Vui lòng nhập email' })
    .email({ message: 'Địa chỉ email không hợp lệ' }),
});

const passwordSchema = z
  .object({
    newPassword: z
      .string()
      .min(8, { message: 'Mật khẩu phải có ít nhất 8 ký tự' })
      .regex(/[0-9]/, { message: 'Mật khẩu phải chứa ít nhất một chữ số' })
      .regex(/[^a-zA-Z0-9]/, { message: 'Mật khẩu phải chứa ít nhất một ký tự đặc biệt' }),
    confirmPassword: z
      .string()
      .min(1, { message: 'Vui lòng xác nhận mật khẩu mới' }),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  });

export default function ForgotPassword({ setActivePage }) {
  const [step, setStep] = useState(1); // 1: Email Input, 2: OTP Input, 3: New Password, 4: Success
  const [isLoading, setIsLoading] = useState(false);
  const [apiError, setApiError] = useState(null);

  // States to keep data across steps
  const [userEmail, setUserEmail] = useState('');
  const [resetToken, setResetToken] = useState('');

  // OTP state
  const [otp, setOtp] = useState('');
  const [countdown, setCountdown] = useState(60);

  // Toast notification
  const { toasts, showToast, dismissToast } = useToast();

  // React Hook Forms
  const {
    register: registerEmail,
    handleSubmit: handleEmailSubmit,
    formState: { errors: emailErrors },
  } = useForm({
    resolver: zodResolver(emailSchema),
    mode: 'onChange',
  });

  const {
    register: registerPassword,
    handleSubmit: handlePasswordSubmit,
    formState: { errors: passwordErrors },
  } = useForm({
    resolver: zodResolver(passwordSchema),
    mode: 'onChange',
  });

  // Step 2 Countdown timer
  useEffect(() => {
    let timer;
    if (step === 2 && countdown > 0) {
      timer = setInterval(() => {
        setCountdown((prev) => prev - 1);
      }, 1000);
    }
    return () => clearInterval(timer);
  }, [step, countdown]);

  // ----- Step 1: Request Password Reset (Send OTP) -----
  const onEmailSubmit = async (data) => {
    setIsLoading(true);
    setApiError(null);
    try {
      await forgotPassword({ email: data.email });
      setUserEmail(data.email);
      showToast('Mã OTP đã được gửi đến email của bạn.', 'success');
      setStep(2);
      setCountdown(60);
      setOtp('');
    } catch (err) {
      const msg = err?.response?.data?.message || 'Không thể gửi yêu cầu. Vui lòng thử lại sau.';
      setApiError(msg);
      showToast(msg, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  // ----- Step 2: Verify OTP -----
  const onOtpSubmit = async (e) => {
    e.preventDefault();
    if (otp.length < 6) {
      setApiError('Vui lòng nhập đầy đủ mã xác thực OTP 6 chữ số.');
      return;
    }

    setIsLoading(true);
    setApiError(null);
    try {
      const response = await verifyForgotOTP({ email: userEmail, otp });
      // The response.data should contain the resetToken
      setResetToken(response.data);
      showToast('Xác thực mã OTP thành công.', 'success');
      setStep(3);
    } catch (err) {
      const msg = err?.response?.data?.message || 'Mã OTP không hợp lệ hoặc đã hết hạn.';
      setApiError(msg);
      showToast(msg, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  // ----- Step 3: Reset Password -----
  const onPasswordSubmit = async (data) => {
    setIsLoading(true);
    setApiError(null);
    try {
      await resetPassword({
        resetToken: resetToken,
        newPassword: data.newPassword,
      });
      showToast('Đặt lại mật khẩu thành công.', 'success');
      setStep(4);
      setTimeout(() => setActivePage('login'), 2500);
    } catch (err) {
      const msg = err?.response?.data?.message || 'Đặt lại mật khẩu thất bại. Vui lòng thử lại.';
      setApiError(msg);
      showToast(msg, 'error');
    } finally {
      setIsLoading(false);
    }
  };

  // ----- Resend OTP -----
  const handleResendOtp = async () => {
    if (countdown > 0 || isLoading) return;

    setIsLoading(true);
    setApiError(null);
    setOtp('');
    try {
      await forgotPassword({ email: userEmail });
      setCountdown(60);
      showToast('Mã OTP mới đã được gửi thành công.', 'success');
    } catch (err) {
      showToast(err?.response?.data?.message || 'Không thể gửi lại OTP. Vui lòng thử lại.', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />
      <div className="w-full min-h-screen pt-36 pb-24 elysian-pattern flex items-center justify-center px-4">
        <div className="bg-white max-w-xl w-full p-8 md:p-12 border border-outline-variant shadow-2xl relative text-left">
          
          {/* Navigation back button */}
          <button 
            onClick={() => {
              if (step === 1) setActivePage('login');
              else if (step === 2) setStep(1);
              else if (step === 3) setStep(2);
            }}
            className="absolute top-6 left-6 text-xs text-secondary hover:text-primary uppercase tracking-widest font-bold flex items-center gap-1 cursor-pointer bg-transparent border-none"
          >
            <span className="material-symbols-outlined text-sm">arrow_back</span> Quay lại
          </button>

          {/* Step Indicator */}
          {step < 4 && (
            <div className="flex justify-between items-center mb-8 border-b border-outline-variant pb-4 mt-4">
              <div className="flex items-center gap-2">
                <span className={`w-6 h-6 flex items-center justify-center text-xs font-bold ${
                  step === 1 ? 'bg-primary text-white' : 'bg-surface-variant text-on-surface'
                }`}>1</span>
                <span className={`text-xs font-bold uppercase tracking-wider ${
                  step === 1 ? 'text-primary' : 'text-secondary'
                }`}>Email</span>
              </div>
              <div className="w-8 h-[1px] bg-outline-variant flex-grow mx-2"></div>
              <div className="flex items-center gap-2">
                <span className={`w-6 h-6 flex items-center justify-center text-xs font-bold ${
                  step === 2 ? 'bg-primary text-white' : 'bg-surface-variant text-on-surface'
                }`}>2</span>
                <span className={`text-xs font-bold uppercase tracking-wider ${
                  step === 2 ? 'text-primary' : 'text-secondary'
                }`}>Xác thực</span>
              </div>
              <div className="w-8 h-[1px] bg-outline-variant flex-grow mx-2"></div>
              <div className="flex items-center gap-2">
                <span className={`w-6 h-6 flex items-center justify-center text-xs font-bold ${
                  step === 3 ? 'bg-primary text-white' : 'bg-surface-variant text-on-surface'
                }`}>3</span>
                <span className={`text-xs font-bold uppercase tracking-wider ${
                  step === 3 ? 'text-primary' : 'text-secondary'
                }`}>Mật khẩu</span>
              </div>
            </div>
          )}

          {/* Title */}
          <div className="mb-8">
            <h2 className="font-headline-lg text-headline-md text-primary uppercase italic m-0">
              {step === 1 && 'QUÊN MẬT KHẨU'}
              {step === 2 && 'NHẬP MÃ XÁC THỰC'}
              {step === 3 && 'ĐẶT LẠI MẬT KHẨU'}
              {step === 4 && 'THÀNH CÔNG'}
            </h2>
            <p className="text-secondary text-sm mt-2">
              {step === 1 && 'Nhập email đã đăng ký hội viên của bạn để thiết lập lại mật khẩu.'}
              {step === 2 && `Mã xác thực OTP đã được gửi đến địa chỉ email: ${userEmail}`}
              {step === 3 && 'Vui lòng thiết lập mật khẩu mới có độ bảo mật cao.'}
              {step === 4 && 'Mật khẩu của bạn đã được cập nhật thành công.'}
            </p>
          </div>

          {/* Dynamic API Error message box */}
          {apiError && (
            <div className="bg-error-container text-on-error-container border border-error p-4 mb-6 flex items-start gap-3">
              <span className="material-symbols-outlined text-xl mt-0.5">error</span>
              <div className="text-sm font-semibold leading-relaxed">{apiError}</div>
            </div>
          )}

          {/* Step 1 Form */}
          {step === 1 && (
            <form onSubmit={handleEmailSubmit(onEmailSubmit)} className="space-y-6">
              <div className="space-y-2">
                <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Email hội viên</label>
                <input 
                  {...registerEmail('email')}
                  type="email" 
                  placeholder="name@example.com" 
                  disabled={isLoading}
                  className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary disabled:opacity-50"
                />
                {emailErrors.email && (
                  <p className="text-xs text-error font-medium mt-1">{emailErrors.email.message}</p>
                )}
              </div>

              <button 
                type="submit"
                disabled={isLoading}
                className="w-full bg-primary text-on-primary font-bold py-4 uppercase tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center gap-2 h-12"
              >
                {isLoading ? (
                  <>
                    <span className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                    ĐANG GỬI...
                  </>
                ) : 'TIẾP TỤC'}
              </button>
            </form>
          )}

          {/* Step 2 Form */}
          {step === 2 && (
            <form onSubmit={onOtpSubmit} className="space-y-8">
              <div className="space-y-4">
                <label className="block text-center text-xs font-bold text-secondary uppercase tracking-widest">
                  Nhập mã xác thực 6 chữ số
                </label>
                <OtpInput value={otp} onChange={setOtp} disabled={isLoading} />
              </div>

              <div className="flex flex-col items-center gap-3">
                {countdown > 0 ? (
                  <p className="text-sm text-secondary font-semibold">
                    Mã xác thực hết hạn sau <span className="text-primary font-bold">{countdown}s</span>
                  </p>
                ) : (
                  <button
                    type="button"
                    onClick={handleResendOtp}
                    disabled={isLoading}
                    className="text-xs text-primary font-bold uppercase tracking-widest hover:underline cursor-pointer bg-transparent border-none disabled:opacity-30 disabled:no-underline"
                  >
                    GỬI LẠI MÃ OTP
                  </button>
                )}
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full bg-primary text-on-primary font-bold py-4 uppercase tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center gap-2 h-12"
              >
                {isLoading ? (
                  <>
                    <span className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                    ĐANG XÁC THỰC...
                  </>
                ) : 'XÁC THỰC OTP'}
              </button>
            </form>
          )}

          {/* Step 3 Form */}
          {step === 3 && (
            <form onSubmit={handlePasswordSubmit(onPasswordSubmit)} className="space-y-6">
              <div className="space-y-2">
                <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Mật khẩu mới</label>
                <input 
                  {...registerPassword('newPassword')}
                  type="password" 
                  placeholder="••••••" 
                  disabled={isLoading}
                  className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary disabled:opacity-50"
                />
                {passwordErrors.newPassword && (
                  <p className="text-xs text-error font-medium mt-1">{passwordErrors.newPassword.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Xác nhận mật khẩu mới</label>
                <input 
                  {...registerPassword('confirmPassword')}
                  type="password" 
                  placeholder="••••••" 
                  disabled={isLoading}
                  className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary disabled:opacity-50"
                />
                {passwordErrors.confirmPassword && (
                  <p className="text-xs text-error font-medium mt-1">{passwordErrors.confirmPassword.message}</p>
                )}
              </div>

              <button 
                type="submit"
                disabled={isLoading}
                className="w-full bg-primary text-on-primary font-bold py-4 uppercase tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center gap-2 h-12"
              >
                {isLoading ? (
                  <>
                    <span className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                    ĐANG ĐẶT LẠI MẬT KHẨU...
                  </>
                ) : 'ĐẶT LẠI MẬT KHẨU'}
              </button>
            </form>
          )}

          {/* Step 4: Success */}
          {step === 4 && (
            <div className="text-center py-6 space-y-6">
              <div className="w-20 h-20 bg-primary/10 border border-primary/20 text-primary rounded-full flex items-center justify-center mx-auto">
                <span className="material-symbols-outlined text-4xl font-bold">check_circle</span>
              </div>
              
              <div className="space-y-2">
                <h3 className="font-bold text-xl text-on-surface uppercase">Thiết lập lại mật khẩu thành công</h3>
                <p className="text-sm text-secondary max-w-sm mx-auto leading-relaxed">
                  Mật khẩu mới của bạn đã được ghi nhận. Bạn sẽ được tự động chuyển về trang đăng nhập sau vài giây.
                </p>
              </div>

              <button
                onClick={() => setActivePage('login')}
                className="w-full max-w-xs bg-primary text-on-primary font-bold py-4 uppercase tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none mx-auto block h-12 flex items-center justify-center"
              >
                ĐĂNG NHẬP NGAY
              </button>
            </div>
          )}

        </div>
      </div>
    </>
  );
}
