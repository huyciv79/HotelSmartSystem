import { useState, useRef, useEffect } from 'react';
import { Camera, User, Mail, Phone, MapPin, Award, Check, X, IdCard } from 'lucide-react';
import { updateUserProfile, uploadAvatar } from '../services/userService';
import { useLanguage } from '../context/LanguageContext';

export default function Profile({ initialProfile, onProfileUpdate, showToast }) {
  const { t } = useLanguage();
  const [isEditMode, setIsEditMode] = useState(false);
  const [profile, setProfile] = useState(initialProfile || {});
  const [formData, setFormData] = useState({
    fullName: initialProfile?.fullName || '',
    address: initialProfile?.address || '',
  });
  const [avatarPreview, setAvatarPreview] = useState(initialProfile?.avatar || initialProfile?.avatarUrl || '');
  const [isSaving, setIsSaving] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const fileInputRef = useRef(null);

  // Sync state if initialProfile changes
  useEffect(() => {
    if (initialProfile) {
      setTimeout(() => {
        setProfile(initialProfile);
        setFormData({
          fullName: initialProfile.fullName || '',
          address: initialProfile.address || '',
        });
        setAvatarPreview(initialProfile.avatar || initialProfile.avatarUrl || '');
      }, 0);
    }
  }, [initialProfile]);

  // Handle input changes
  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  // Determine if form is dirty (has modifications)
  const isDirty =
    formData.fullName !== (profile.fullName || '') ||
    formData.address !== (profile.address || '');

  // Reset form when cancelling edit mode
  const handleCancel = () => {
    setFormData({
      fullName: profile.fullName || '',
      address: profile.address || '',
    });
    setIsEditMode(false);
  };

  // Submit profile details update
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.fullName.trim()) {
      showToast(t('profile_toast_fullname_empty', 'Họ và tên không được để trống'), 'error');
      return;
    }

    setIsSaving(true);
    try {
      const response = await updateUserProfile(formData);
      if (response && response.data) {
        setProfile(response.data);
        onProfileUpdate?.(response.data);
        showToast(t('profile_toast_update_success', 'Cập nhật hồ sơ thành công!'), 'success');
      } else {
        showToast(t('profile_toast_update_success', 'Cập nhật hồ sơ thành công!'), 'success');
      }
      setIsEditMode(false);
    } catch (err) {
      const errMsg = t(err?.response?.data?.message, t('profile_toast_update_error', 'Có lỗi xảy ra khi cập nhật hồ sơ.'));
      showToast(errMsg, 'error');
    } finally {
      setIsSaving(false);
    }
  };

  // Avatar Click Handler
  const handleAvatarClick = () => {
    fileInputRef.current?.click();
  };

  // Avatar File Change Handler
  const handleFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
    const fileExtension = file.name.split('.').pop().toLowerCase();
    const isValidExtension = ['jpg', 'jpeg', 'png', 'webp'].includes(fileExtension);

    if (!validTypes.includes(file.type) && !isValidExtension) {
      showToast(t('profile_toast_avatar_invalid_type', 'Định dạng tệp không hợp lệ. Chỉ chấp nhận .jpg, .png, .webp'), 'error');
      return;
    }

    const maxSize = 5 * 1024 * 1024;
    if (file.size > maxSize) {
      showToast(t('profile_toast_avatar_too_large', 'Kích thước tệp tối đa là 5 MB'), 'error');
      return;
    }

    const previewUrl = URL.createObjectURL(file);
    setAvatarPreview(previewUrl);
    setIsUploading(true);

    try {
      const response = await uploadAvatar(file);
      const newAvatarUrl = response?.data || response?.avatarUrl;

      if (newAvatarUrl) {
        const updatedProfile = { ...profile, avatar: newAvatarUrl };
        setProfile(updatedProfile);
        onProfileUpdate?.(updatedProfile);
        showToast(t('profile_toast_avatar_success', 'Tải lên ảnh đại diện thành công!'), 'success');
      } else {
        showToast(t('profile_toast_avatar_success', 'Tải lên ảnh đại diện thành công!'), 'success');
      }
    } catch (err) {
      setAvatarPreview(profile.avatar || profile.avatarUrl || '');
      const errMsg = t(err?.response?.data?.message, t('profile_toast_avatar_error', 'Có lỗi xảy ra khi tải ảnh đại diện lên.'));
      showToast(errMsg, 'error');
    } finally {
      setIsUploading(false);
    }
  };

  // Format joined date
  const formatDate = (isoString) => {
    if (!isoString) return t('profile_member_new', 'Thành viên mới');
    try {
      const date = new Date(isoString);
      return date.toLocaleDateString(t('locale_format', 'vi-VN'), {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      });
    } catch {
      return t('profile_member_new', 'Thành viên mới');
    }
  };

  return (
    <div className="elysian-pattern -m-8 p-8 flex-1 min-h-[calc(100vh-80px)] animate-fade-in font-['Montserrat'] select-none text-left">
      <div className="max-w-6xl mx-auto">
        {/* Title Block */}
        <div className="mb-10 text-left border-b border-white/20 pb-6">
          <h2 className="font-headline-lg text-2xl md:text-3xl text-white uppercase italic m-0 tracking-widest font-black leading-none text-shadow-hard">
            {t('profile_title', 'HỒ SƠ CÁ NHÂN')}
          </h2>
          <p className="text-white/80 text-xs uppercase tracking-widest mt-3 font-semibold">
            {t('profile_subtitle', 'Quản lý thông tin tài khoản')}
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-stretch">
          {/* Left Column: Membership Card */}
          <div className="lg:col-span-1 flex flex-col gap-6 h-full">
            <div className="bg-white text-on-surface rounded-none overflow-hidden shadow-xl border border-outline-variant relative flex flex-col h-full">
              <div className="bg-transparent p-8 flex flex-col items-center text-center relative z-10">

              {/* Avatar Uploader */}
              <div className="relative group cursor-pointer mb-6" onClick={handleAvatarClick}>
                <div className="size-32 rounded-full overflow-hidden border-2 border-outline-variant shadow-md relative bg-slate-100 flex items-center justify-center">
                  {avatarPreview ? (
                    <img
                      src={avatarPreview}
                      alt={profile.fullName}
                      className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                    />
                  ) : (
                    <User className="size-16 text-slate-400" />
                  )}

                  {/* Upload Overlay */}
                  <div className="absolute inset-0 bg-[#a20513]/85 opacity-0 group-hover:opacity-100 transition-opacity flex flex-col items-center justify-center gap-1 text-white">
                    <Camera size={18} className="text-white" />
                    <span className="text-[9px] uppercase tracking-widest font-black">{t('profile_avatar_change', 'Thay ảnh')}</span>
                  </div>

                  {/* Uploading Spinner */}
                  {isUploading && (
                    <div className="absolute inset-0 bg-[#a20513]/90 flex items-center justify-center">
                      <span className="w-6 h-6 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                    </div>
                  )}
                </div>

                <div className="absolute bottom-1 right-1 bg-white text-primary p-2 rounded-full shadow-md border border-[#a20513]">
                  <Camera size={12} />
                </div>
              </div>

              <input
                type="file"
                ref={fileInputRef}
                onChange={handleFileChange}
                accept=".jpg,.jpeg,.png,.webp"
                className="hidden"
              />

              {/* Member Name & Role */}
              <h3 className="text-base font-bold font-['Playfair_Display'] text-on-surface tracking-widest m-0 uppercase leading-normal">
                {profile.fullName || t('profile_member_default', 'Hội viên Elysian')}
              </h3>
              <div className="w-full h-px bg-outline-variant my-6" />

              <div className="w-full space-y-1 text-[10px] uppercase tracking-widest font-bold">
                <div className="flex items-center justify-between py-2">
                  <span className="text-secondary">{t('profile_joined_date', 'Gia nhập')}</span>
                  <span className="text-on-surface">
                    {formatDate(profile.createdAt)}
                  </span>
                </div>
              </div>
            </div>

            {/* Card Footer */}
            <div className="bg-slate-50 px-8 py-3.5 border-t border-outline-variant flex justify-between items-center text-[9px]">
              <span className="text-secondary tracking-widest font-black font-mono">ELYSIAN HOTELS & RESORTS</span>
              <Award size={14} className="text-secondary" />
            </div>
          </div>
        </div>

        {/* Right Column: Detailed Info Form & Change Password */}
        <div className="lg:col-span-2 flex flex-col gap-8">
          <div className="bg-white rounded-none p-6 md:p-8 border border-outline-variant shadow-lg text-left">
            <div className="flex justify-between items-center mb-8 border-b border-outline-variant pb-4">
              <div>
                <h3 className="text-xs font-bold uppercase tracking-widest text-primary font-['Montserrat']">
                  {t('profile_info_title', 'Thông Tin Hội Viên')}
                </h3>
                <p className="text-xs text-secondary mt-1.5 font-semibold opacity-85">{t('profile_info_subtitle', 'Cập nhật hồ sơ để nhận các ưu đãi concierge cá nhân hóa')}</p>
              </div>

              {!isEditMode && (
                <button
                  type="button"
                  onClick={() => setIsEditMode(true)}
                  className="border border-on-surface text-on-surface font-bold text-[9px] tracking-widest uppercase px-5 py-2.5 hover:bg-slate-900 hover:text-white transition-all cursor-pointer rounded-none bg-transparent"
                >
                  {t('profile_btn_edit', 'Chỉnh Sửa')}
                </button>
              )}
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Full Name */}
                <div className="space-y-2 text-left">
                  <label className="block text-[9px] font-bold text-secondary uppercase tracking-widest flex items-center gap-1.5 select-none opacity-80">
                    <User size={13} className="text-slate-400" />
                    {t('profile_label_fullname', 'Họ và tên')}
                  </label>
                  {isEditMode ? (
                    <input
                      type="text"
                      name="fullName"
                      value={formData.fullName}
                      onChange={handleInputChange}
                      required
                      placeholder={t('profile_placeholder_fullname', 'Nhập họ và tên')}
                      className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary transition-colors text-slate-800"
                    />
                  ) : (
                    <div className="border-b border-slate-200 py-2 font-bold text-sm tracking-wide text-slate-800 min-h-[38px] flex items-center">
                      {profile.fullName || t('profile_update_missing', 'Chưa cập nhật')}
                    </div>
                  )}
                </div>

                {/* Email (Read Only) */}
                <div className="space-y-2 text-left">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest flex items-center gap-1.5 select-none opacity-80">
                    <Mail size={13} className="text-slate-400" />
                    {t('profile_label_email', 'Địa chỉ Email')}
                  </label>
                  <div className="border-b border-slate-200/50 py-2 font-bold text-sm tracking-wide text-slate-500 flex items-center justify-between min-h-[38px]">
                    <span>{profile.email || t('profile_update_missing', 'Chưa cập nhật')}</span>
                    <span className="text-[7px] font-black text-slate-400 uppercase tracking-widest border border-slate-200 px-1.5 py-0.5 select-none">{t('profile_label_fixed', 'CỐ ĐỊNH')}</span>
                  </div>
                </div>

                {/* Phone Number (Read Only) */}
                <div className="space-y-2 text-left">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest flex items-center gap-1.5 select-none opacity-80">
                    <Phone size={13} className="text-slate-400" />
                    {t('profile_label_phone', 'Số điện thoại')}
                  </label>
                  <div className="border-b border-slate-200/50 py-2 font-bold text-sm tracking-wide text-slate-500 flex items-center justify-between min-h-[38px]">
                    <span>{profile.phoneNumber || t('profile_update_missing', 'Chưa cập nhật')}</span>
                    <span className="text-[7px] font-black text-slate-400 uppercase tracking-widest border border-slate-200 px-1.5 py-0.5 select-none">{t('profile_label_fixed', 'CỐ ĐỊNH')}</span>
                  </div>
                </div>

                {/* ID Card Number (Read Only) */}
                <div className="space-y-2 text-left">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest flex items-center gap-1.5 select-none opacity-80">
                    <IdCard size={13} className="text-slate-400" />
                    {t('profile_label_idcard', 'So CCCD')}
                  </label>
                  <div className="border-b border-slate-200/50 py-2 font-bold text-sm tracking-wide text-slate-500 flex items-center justify-between min-h-[38px]">
                    <span>{profile.idCardNumber || t('profile_update_missing', 'Chưa cập nhật')}</span>
                    <span className="text-[7px] font-black text-slate-400 uppercase tracking-widest border border-slate-200 px-1.5 py-0.5 select-none">{t('profile_label_fixed', 'CỐ ĐỊNH')}</span>
                  </div>
                </div>

                {/* Address */}
                <div className="space-y-2 md:col-span-2 text-left">
                  <label className="block text-[9px] font-bold text-secondary uppercase tracking-widest flex items-center gap-1.5 select-none opacity-80">
                    <MapPin size={13} className="text-slate-400" />
                    {t('profile_label_address', 'Địa chỉ liên hệ')}
                  </label>
                  {isEditMode ? (
                    <input
                      type="text"
                      name="address"
                      value={formData.address}
                      onChange={handleInputChange}
                      placeholder={t('profile_placeholder_address', 'Nhập địa chỉ của bạn')}
                      className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary transition-colors text-slate-800"
                    />
                  ) : (
                    <div className="border-b border-slate-200 py-2 font-bold text-sm tracking-wide text-slate-800 min-h-[38px] flex items-center">
                      {profile.address || t('profile_missing_address', 'Chưa cập nhật địa chỉ')}
                    </div>
                  )}
                </div>
              </div>

              {/* Form Buttons */}
              {isEditMode && (
                <div className="flex justify-end gap-4 pt-6 border-t border-slate-100 mt-8">
                  <button
                    type="button"
                    onClick={handleCancel}
                    disabled={isSaving}
                    className="px-6 py-3 border border-secondary text-secondary font-bold text-[9px] tracking-widest uppercase hover:bg-slate-50 disabled:opacity-50 transition-all cursor-pointer bg-transparent rounded-none flex items-center gap-1.5"
                  >
                    <X size={12} />
                    {t('profile_btn_cancel', 'Hủy bỏ')}
                  </button>

                  <button
                    type="submit"
                    disabled={!isDirty || isSaving}
                    className={`px-6 py-3 font-bold text-[9px] tracking-widest uppercase rounded-none flex items-center gap-1.5 transition-all cursor-pointer border-none ${isDirty && !isSaving
                      ? 'bg-primary text-on-primary hover:brightness-110 shadow-md'
                      : 'bg-slate-200 text-slate-400 cursor-not-allowed'
                      }`}
                  >
                    {isSaving ? (
                      <>
                        <span className="w-3.5 h-3.5 border-2 border-slate-400/30 border-t-slate-800 rounded-full animate-spin"></span>
                        {t('profile_btn_saving', 'ĐANG LƯU...')}
                      </>
                    ) : (
                      <>
                        <Check size={12} />
                        {t('profile_btn_save_changes', 'LƯU THAY ĐỔI')}
                      </>
                    )}
                  </button>
                </div>
              )}
            </form>
          </div>


        </div>
      </div>
    </div>
  </div>
  );
}
