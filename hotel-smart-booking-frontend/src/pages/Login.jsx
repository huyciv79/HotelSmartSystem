import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { loginUser } from '../services/authService';
import { useToast, ToastContainer } from '../components/Toast';

const loginSchema = z.object({
  email: z
    .string()
    .min(1, { message: 'Vui lòng nhập email' })
    .email({ message: 'Địa chỉ email không hợp lệ' }),
  password: z
    .string()
    .min(1, { message: 'Vui lòng nhập mật khẩu' }),
});

export default function Login({ setActivePage }) {
  const [isLoading, setIsLoading] = useState(false);
  const [apiError, setApiError] = useState(null);
  const [showPassword, setShowPassword] = useState(false);

  // Toast notification
  const { toasts, showToast, dismissToast } = useToast();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(loginSchema),
    mode: 'onChange',
  });

  const onLoginSubmit = async (data) => {
    setIsLoading(true);
    setApiError(null);
    try {
      const response = await loginUser(data);
      
      // Save tokens/user information to localStorage
      if (response && response.data) {
        const { accessToken, refreshToken, user } = response.data;
        if (accessToken) localStorage.setItem('accessToken', accessToken);
        if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
        if (user) localStorage.setItem('user', JSON.stringify(user));
      }

      showToast('Đăng nhập thành công!', 'success');
      
      setTimeout(() => {
        setActivePage('home');
      }, 1500);
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        'Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin đăng nhập.';
      setApiError(msg);
      showToast(msg, 'error');
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
            onClick={() => setActivePage('home')}
            className="absolute top-6 left-6 text-xs text-secondary hover:text-primary uppercase tracking-widest font-bold flex items-center gap-1 cursor-pointer bg-transparent border-none"
          >
            <span className="material-symbols-outlined text-sm">arrow_back</span> Quay lại
          </button>

          {/* Title */}
          <div className="mb-8 mt-4">
            <h2 className="font-headline-lg text-headline-md text-primary uppercase italic m-0">
              ĐĂNG NHẬP HỘI VIÊN
            </h2>
            <p className="text-secondary text-sm mt-2">
              Đăng nhập để quản lý đặt phòng và tận hưởng ưu đãi dành riêng cho hội viên Elysian.
            </p>
          </div>

          {/* Dynamic API Error message box */}
          {apiError && (
            <div className="bg-error-container text-on-error-container border border-error p-4 mb-6 flex items-start gap-3">
              <span className="material-symbols-outlined text-xl mt-0.5">error</span>
              <div className="text-sm font-semibold leading-relaxed">{apiError}</div>
            </div>
          )}

          {/* Login Form */}
          <form onSubmit={handleSubmit(onLoginSubmit)} className="space-y-6">
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

            <div className="space-y-2 relative">
              <label className="block text-xs font-bold text-secondary uppercase tracking-widest">Mật khẩu</label>
              <div className="relative flex items-center">
                <input 
                  {...register('password')}
                  type={showPassword ? 'text' : 'password'} 
                  placeholder="••••••" 
                  disabled={isLoading}
                  className="w-full bg-transparent border-b border-on-surface py-2 pr-10 font-bold text-sm outline-none focus:border-primary disabled:opacity-50"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-0 bottom-2 text-secondary hover:text-primary cursor-pointer bg-transparent border-none flex items-center"
                >
                  <span className="material-symbols-outlined text-lg">
                    {showPassword ? 'visibility_off' : 'visibility'}
                  </span>
                </button>
              </div>
              {errors.password && (
                <p className="text-xs text-error font-medium mt-1">{errors.password.message}</p>
              )}
            </div>

            <div className="flex justify-between items-center text-xs font-bold uppercase tracking-wider py-2">
              <button 
                type="button"
                onClick={() => setActivePage('register')}
                className="text-secondary hover:text-primary transition-colors cursor-pointer bg-transparent border-none"
              >
                Chưa có tài khoản? Đăng ký ngay
              </button>
              <button 
                type="button"
                onClick={() => setActivePage('forgot-password')}
                className="text-secondary hover:text-primary transition-colors cursor-pointer bg-transparent border-none"
              >
                Quên mật khẩu?
              </button>
            </div>

            <button 
              type="submit"
              disabled={isLoading}
              className="w-full bg-primary text-on-primary font-bold py-4 uppercase tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center gap-2 h-12"
            >
              {isLoading ? (
                <>
                  <span className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                  ĐANG ĐĂNG NHẬP...
                </>
              ) : 'ĐĂNG NHẬP'}
            </button>
          </form>

        </div>
      </div>
    </>
  );
}
