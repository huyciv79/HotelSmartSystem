import { useState } from 'react';
import { Eye, EyeOff, Lock, Check, X } from 'lucide-react';
import { changePassword } from '../services/authService';

export default function ChangePassword({ onCancel, showToast }) {
  const [formData, setFormData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  // Handle Input Changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  // Submit password change
  const handleSubmit = async (e) => {
    e.preventDefault();

    const { currentPassword, newPassword, confirmPassword } = formData;

    // Validation
    if (!currentPassword) {
      showToast('Vui lòng nhập mật khẩu hiện tại', 'error');
      return;
    }
    if (!newPassword) {
      showToast('Vui lòng nhập mật khẩu mới', 'error');
      return;
    }
    if (newPassword.length < 8) {
      showToast('Mật khẩu mới phải có ít nhất 8 ký tự', 'error');
      return;
    }
    if (!/[0-9]/.test(newPassword)) {
      showToast('Mật khẩu mới phải chứa ít nhất một chữ số', 'error');
      return;
    }
    if (!/[^a-zA-Z0-9]/.test(newPassword)) {
      showToast('Mật khẩu mới phải chứa ít nhất một ký tự đặc biệt', 'error');
      return;
    }
    if (newPassword === currentPassword) {
      showToast('Mật khẩu mới không được trùng với mật khẩu hiện tại', 'error');
      return;
    }
    if (newPassword !== confirmPassword) {
      showToast('Mật khẩu xác nhận không khớp', 'error');
      return;
    }

    setIsSaving(true);
    try {
      await changePassword({ currentPassword, newPassword });
      showToast('Đổi mật khẩu thành công!', 'success');
      // Reset form
      setFormData({
        currentPassword: '',
        newPassword: '',
        confirmPassword: '',
      });
      setTimeout(() => {
        onCancel?.();
      }, 1000);
    } catch (err) {
      const errMsg = err?.response?.data?.message || 'Có lỗi xảy ra khi đổi mật khẩu.';
      showToast(errMsg, 'error');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="max-w-xl mx-auto p-4 md:p-8 animate-fade-in font-['Montserrat'] text-left">
      {/* Title */}
      <div className="mb-8 border-b border-outline-variant pb-6">
        <h2 className="font-headline-lg text-2xl text-primary uppercase italic m-0 tracking-widest font-black leading-none">
          ĐỔI MẬT KHẨU
        </h2>
        <p className="text-secondary text-xs uppercase tracking-widest mt-3 font-bold opacity-80 select-none">
          Thay đổi mật khẩu định kỳ để bảo vệ tài khoản của bạn
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Current Password */}
        <div className="space-y-2 relative">
          <label className="block text-[9px] font-bold text-secondary uppercase tracking-widest flex items-center gap-1.5 select-none opacity-80">
            <Lock size={13} className="text-slate-400" />
            Mật khẩu hiện tại
          </label>
          <div className="relative flex items-center">
            <input
              type={showCurrentPassword ? 'text' : 'password'}
              name="currentPassword"
              value={formData.currentPassword}
              onChange={handleInputChange}
              required
              placeholder="••••••"
              className="w-full bg-transparent border-b border-on-surface py-2 pr-10 font-bold text-sm outline-none focus:border-primary transition-colors text-slate-800"
            />
            <button
              type="button"
              onClick={() => setShowCurrentPassword(!showCurrentPassword)}
              className="absolute right-0 bottom-2 text-slate-450 hover:text-primary cursor-pointer bg-transparent border-none flex items-center"
            >
              {showCurrentPassword ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
        </div>

        {/* New Password */}
        <div className="space-y-2 relative">
          <label className="block text-[9px] font-bold text-secondary uppercase tracking-widest flex items-center gap-1.5 select-none opacity-80">
            <Lock size={13} className="text-slate-400" />
            Mật khẩu mới
          </label>
          <div className="relative flex items-center">
            <input
              type={showNewPassword ? 'text' : 'password'}
              name="newPassword"
              value={formData.newPassword}
              onChange={handleInputChange}
              required
              placeholder="••••••"
              className="w-full bg-transparent border-b border-on-surface py-2 pr-10 font-bold text-sm outline-none focus:border-primary transition-colors text-slate-800"
            />
            <button
              type="button"
              onClick={() => setShowNewPassword(!showNewPassword)}
              className="absolute right-0 bottom-2 text-slate-450 hover:text-primary cursor-pointer bg-transparent border-none flex items-center"
            >
              {showNewPassword ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
        </div>

        {/* Confirm Password */}
        <div className="space-y-2 relative">
          <label className="block text-[9px] font-bold text-secondary uppercase tracking-widest flex items-center gap-1.5 select-none opacity-80">
            <Lock size={13} className="text-slate-400" />
            Xác nhận mật khẩu mới
          </label>
          <div className="relative flex items-center">
            <input
              type={showConfirmPassword ? 'text' : 'password'}
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleInputChange}
              required
              placeholder="••••••"
              className="w-full bg-transparent border-b border-on-surface py-2 pr-10 font-bold text-sm outline-none focus:border-primary transition-colors text-slate-800"
            />
            <button
              type="button"
              onClick={() => setShowConfirmPassword(!showConfirmPassword)}
              className="absolute right-0 bottom-2 text-slate-450 hover:text-primary cursor-pointer bg-transparent border-none flex items-center"
            >
              {showConfirmPassword ? <EyeOff size={16} /> : <Eye size={16} />}
            </button>
          </div>
        </div>

        {/* Buttons */}
        <div className="flex justify-end gap-4 pt-6 border-t border-slate-100 mt-8">
          <button
            type="button"
            onClick={onCancel}
            disabled={isSaving}
            className="px-6 py-3 border border-secondary text-secondary font-bold text-[9px] tracking-widest uppercase hover:bg-slate-50 disabled:opacity-50 transition-all cursor-pointer bg-transparent rounded-none flex items-center gap-1.5"
          >
            <X size={12} />
            HỦY BỎ
          </button>

          <button
            type="submit"
            disabled={isSaving}
            className="px-6 py-3 bg-primary text-on-primary font-bold text-[9px] tracking-widest uppercase rounded-none flex items-center gap-1.5 transition-all cursor-pointer border-none hover:brightness-110 shadow-md"
          >
            {isSaving ? (
              <>
                <span className="w-3.5 h-3.5 border-2 border-slate-400/30 border-t-slate-800 rounded-full animate-spin"></span>
                ĐANG LƯU...
              </>
            ) : (
              <>
                <Check size={12} />
                LƯU THAY ĐỔI
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
