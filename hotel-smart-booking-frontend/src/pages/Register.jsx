import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import OtpInput from '../components/OtpInput';
import { registerUser, verifyOTP } from '../services/authService';
import { useToast, ToastContainer } from '../components/Toast';

const registerSchema = z
  .object({
    fullName: z
      .string()
      .min(2, { message: 'Họ và tên phải có ít nhất 2 ký tự' }),

    email: z
      .string()
      .min(1, { message: 'Vui lòng nhập email' })
      .email({ message: 'Địa chỉ email không hợp lệ' }),

    // Supports both 0xxxxxxxxx and +84xxxxxxxxx Vietnamese phone formats
    phone: z
      .string()
      .min(1, { message: 'Vui lòng nhập số điện thoại' })
      .regex(
        /^(\+84|0)(3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])[0-9]{7}$/,
        { message: 'Số điện thoại Việt Nam không hợp lệ (VD: 0912345678 hoặc +84912345678)' }
      ),

    password: z
      .string()
      .min(8, { message: 'Mật khẩu phải có ít nhất 8 ký tự' })
      .regex(/[0-9]/, { message: 'Mật khẩu phải chứa ít nhất một chữ số' })
      .regex(/[^a-zA-Z0-9]/, { message: 'Mật khẩu phải chứa ít nhất một ký tự đặc biệt' }),

    confirm: z
      .string()
      .min(1, { message: 'Vui lòng xác nhận mật khẩu' }),
  })
  .refine((data) => data.password === data.confirm, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirm'],
  });

export default function Register({ setActivePage }) {
  const [step, setStep] = useState(1); // 1: Personal Info, 2: OTP, 3: Success
  const [isLoading, setIsLoading] = useState(false);
  const [apiError, setApiError] = useState(null);

  // Store submitted email to use in OTP verification
  const [registeredEmail, setRegisteredEmail] = useState('');

  // OTP state
  const [otp, setOtp] = useState('');
  const [countdown, setCountdown] = useState(60);

  // Toast notification
  const { toasts, showToast, dismissToast } = useToast();

  const {
    register,
    handleSubmit,
    formState: { errors },
    getValues,
  } = useForm({
    resolver: zodResolver(registerSchema),
    mode: 'onChange', // real-time validation
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

  // ----- Step 1: Register -----
  const onRegisterSubmit = async (data) => {
    setIsLoading(true);
    setApiError(null);
    try {
      await registerUser(data);
      setRegisteredEmail(data.email);
      setStep(2);
      setCountdown(60);
      setOtp('');
    } catch (err) {
      const status = err?.response?.status;
      if (status === 409) {
        const msg = 'Email này đã được đăng ký. Vui lòng sử dụng email khác.';
        setApiError(msg);
        showToast(msg, 'error');
      } else {
        const msg =
          err?.response?.data?.message ||
          'Đăng ký thất bại. Vui lòng thử lại sau.';
        setApiError(msg);
        showToast(msg, 'error');
      }
    } finally {
      setIsLoading(false);
    }
  };

  // ----- Step 2: Verify OTP -----
  const handleOtpSubmit = async (e) => {
    e.preventDefault();

    if (otp.length < 6) {
      setApiError('Vui lòng nhập đầy đủ mã xác thực OTP 6 chữ số.');
      return;
    }

    setIsLoading(true);
    setApiError(null);
    try {
      await verifyOTP({ email: registeredEmail, otp });
      setStep(3);
      // Redirect to login after a short celebration delay
      setTimeout(() => setActivePage('login'), 2500);
    } catch (err) {
      const status = err?.response?.status;
      if (status === 400) {
        setApiError(
          err?.response?.data?.message ||
            'Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.'
        );
      } else {
        setApiError(
          err?.response?.data?.message || 'Xác thực thất bại. Vui lòng thử lại sau.'
        );
      }
    } finally {
      setIsLoading(false);
    }
  };

  // ----- Resend OTP (calls /auth/register again) -----
  const handleResendOtp = async () => {
    if (countdown > 0 || isLoading) return;

    setIsLoading(true);
    setApiError(null);
    setOtp('');
    try {
      const formData = getValues();
      await registerUser(formData);
      setCountdown(60);
      showToast('Mã OTP mới đã được gửi thành công.', 'success');
    } catch (err) {
      showToast(
        err?.response?.data?.message || 'Không thể gửi lại OTP. Vui lòng thử lại.',
        'error'
      );
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
          onClick={() => setActivePage('login')}
          className="absolute top-6 left-6 text-xs text-secondary hover:text-primary uppercase tracking-widest font-bold flex items-center gap-1 cursor-pointer bg-transparent border-none"
        >
          <span className="material-symbols-outlined text-sm">arrow_back</span> Quay lại
        </button>

        {/* Step Indicator */}
        {step < 3 && (
          <div className="flex justify-between items-center mb-8 border-b border-outline-variant pb-4 mt-4">
            <div className="flex items-center gap-2">
              <span className={`w-6 h-6 flex items-center justify-center text-xs font-bold ${
                step === 1 ? 'bg-primary text-white' : 'bg-surface-variant text-on-surface'
              }`}>1</span>
              <span className={`text-xs font-bold uppercase tracking-wider ${
                step === 1 ? 'text-primary' : 'text-secondary'
              }`}>Thông tin</span>
            </div>
            <div className="w-12 h-[1px] bg-outline-variant flex-grow mx-4"></div>
            <div className="flex items-center gap-2">
              <span className={`w-6 h-6 flex items-center justify-center text-xs font-bold ${
                step === 2 ? 'bg-primary text-white' : 'bg-surface-variant text-on-surface'
              }`}>2</span>
              <span className={`text-xs font-bold uppercase tracking-wider ${
                step === 2 ? 'text-primary' : 'text-secondary'
              }`}>Xác thực OTP</span>
            </div>
          </div>
        )}

        {/* Title */}
        <div className="mb-8">
          <h2 className="font-headline-lg text-headline-md text-primary uppercase italic m-0">
            {step === 1 && 'ĐĂNG KÝ HỘI VIÊN'}
            {step === 2 && 'XÁC THỰC TÀI KHOẢN'}
            {step === 3 && 'ĐĂNG KÝ THÀNH CÔNG'}
          </h2>
          <p className="text-secondary text-sm mt-2">
            {step === 1 && 'Gia nhập cộng đồng Elysian Rewards để nhận ngay các ưu đãi đặc quyền.'}
            {step === 2 && 'Mã xác thực OTP đã được gửi tới số điện thoại/email của bạn.'}
            {step === 3 && 'Chúc mừng! Bạn đã chính thức trở thành thành viên Elysian VIP.'}
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
          <form onSubmit={handleSubmit(onRegisterSubmit)} className="space-y-6">
            <div className="space-y-2">
              <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Họ và tên</label>
              <input 
                {...register('fullName')}
                type="text" 
                placeholder="Nguyễn Văn A" 
                disabled={isLoading}
                className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary disabled:opacity-50"
              />
              {errors.fullName && (
                <p className="text-xs text-error font-medium mt-1">{errors.fullName.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Email</label>
              <input 
                {...register('email')}
                type="email" 
                placeholder="name@example.com" 
                disabled={isLoading}
                className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary disabled:opacity-50"
              />
              {errors.email && (
                <p className="text-xs text-error font-medium mt-1">{errors.email.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Số điện thoại</label>
              <input 
                {...register('phone')}
                type="text" 
                placeholder="0987654321" 
                disabled={isLoading}
                className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary disabled:opacity-50"
              />
              {errors.phone && (
                <p className="text-xs text-error font-medium mt-1">{errors.phone.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Mật khẩu</label>
              <input 
                {...register('password')}
                type="password" 
                placeholder="••••••" 
                disabled={isLoading}
                className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary disabled:opacity-50"
              />
              {errors.password && (
                <p className="text-xs text-error font-medium mt-1">{errors.password.message}</p>
              )}
            </div>

            <div className="space-y-2">
              <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Xác nhận mật khẩu</label>
              <input 
                {...register('confirm')}
                type="password" 
                placeholder="••••••" 
                disabled={isLoading}
                className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary disabled:opacity-50"
              />
              {errors.confirm && (
                <p className="text-xs text-error font-medium mt-1">{errors.confirm.message}</p>
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
                  ĐANG XỬ LÝ...
                </>
              ) : 'TIẾP TỤC'}
            </button>
          </form>
        )}

        {/* Step 2 OTP Form */}
        {step === 2 && (
          <form onSubmit={handleOtpSubmit} className="space-y-8">
            <div className="space-y-4">
              <label className="block text-center text-xs font-bold text-secondary uppercase tracking-widest">
                Nhập mã xác thực 6 chữ số
              </label>
              <OtpInput value={otp} onChange={setOtp} disabled={isLoading} />
            </div>

            {/* Timer & Resend Container */}
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
              ) : 'XÁC THỰC ĐĂNG KÝ'}
            </button>
          </form>
        )}

        {/* Step 3 Success */}
        {step === 3 && (
          <div className="text-center py-6 space-y-6">
            <div className="w-20 h-20 bg-primary/10 border border-primary/20 text-primary rounded-full flex items-center justify-center mx-auto">
              <span className="material-symbols-outlined text-4xl font-bold">check_circle</span>
            </div>
            
            <div className="space-y-2">
              <h3 className="font-bold text-xl text-on-surface uppercase">Đăng ký tài khoản thành công</h3>
              <p className="text-sm text-secondary max-w-sm mx-auto leading-relaxed">
                Tài khoản hội viên của bạn đã được kích hoạt. Hãy trải nghiệm kỳ nghỉ dưỡng trọn vẹn tại các khách sạn của Elysian Hotels.
              </p>
            </div>

            <div className="bg-surface-container p-6 border-l-4 border-primary text-left max-w-sm mx-auto">
              <div className="flex gap-3">
                <span className="material-symbols-outlined text-primary text-2xl">card_membership</span>
                <div>
                  <h4 className="font-bold text-sm text-on-surface m-0 uppercase">Hội viên VIP Elysian</h4>
                  <p className="text-xs text-secondary mt-1 m-0">Thẻ thành viên điện tử đã được liên kết với số điện thoại của bạn.</p>
                </div>
              </div>
            </div>

            <button
              onClick={() => setActivePage('home')}
              className="w-full max-w-xs bg-primary text-on-primary font-bold py-4 uppercase tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none mx-auto block h-12 flex items-center justify-center"
            >
              QUAY LẠI TRANG CHỦ
            </button>
          </div>
        )}

      </div>
    </div>
    </>
  );
}
