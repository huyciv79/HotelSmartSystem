import { useState, useRef, useEffect } from 'react';
import { Camera, User, Mail, Phone, MapPin, Award, Calendar, Check, X } from 'lucide-react';
import { getUserProfile, updateUserProfile, uploadAvatar } from '../services/userService';

export default function Profile({ initialProfile, onProfileUpdate, showToast }) {
  const [isEditMode, setIsEditMode] = useState(false);
  const [profile, setProfile] = useState(initialProfile || {});
  const [formData, setFormData] = useState({
    fullName: initialProfile?.fullName || '',
    address: initialProfile?.address || '',
  });
  const [avatarPreview, setAvatarPreview] = useState(initialProfile?.avatar || '');
  const [isSaving, setIsSaving] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const fileInputRef = useRef(null);

  // Sync state if initialProfile changes
  useEffect(() => {
    if (initialProfile) {
      setProfile(initialProfile);
      setFormData({
        fullName: initialProfile.fullName || '',
        address: initialProfile.address || '',
      });
      setAvatarPreview(initialProfile.avatar || '');
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
      showToast('Họ và tên không được để trống', 'error');
      return;
    }

    setIsSaving(true);
    try {
      const response = await updateUserProfile(formData);
      if (response && response.data) {
        setProfile(response.data);
        onProfileUpdate?.(response.data);
        showToast('Cập nhật hồ sơ thành công!', 'success');
      } else {
        showToast('Cập nhật hồ sơ thành công!', 'success');
      }
      setIsEditMode(false);
    } catch (err) {
      const errMsg = err?.response?.data?.message || 'Có lỗi xảy ra khi cập nhật hồ sơ.';
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
      showToast('Định dạng tệp không hợp lệ. Chỉ chấp nhận .jpg, .png, .webp', 'error');
      return;
    }

    const maxSize = 5 * 1024 * 1024;
    if (file.size > maxSize) {
      showToast('Kích thước tệp tối đa là 5 MB', 'error');
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
        showToast('Tải lên ảnh đại diện thành công!', 'success');
      } else {
        showToast('Tải lên ảnh đại diện thành công!', 'success');
      }
    } catch (err) {
      setAvatarPreview(profile.avatar || '');
      const errMsg = err?.response?.data?.message || 'Có lỗi xảy ra khi tải ảnh đại diện lên.';
      showToast(errMsg, 'error');
    } finally {
      setIsUploading(false);
    }
  };

  // Format joined date
  const formatDate = (isoString) => {
    if (!isoString) return 'Thành viên mới';
    try {
      const date = new Date(isoString);
      return date.toLocaleDateString('vi-VN', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      });
    } catch (e) {
      return 'Thành viên mới';
    }
  };

  return (
    <div className="max-w-6xl mx-auto p-4 md:p-8 animate-fade-in font-['Montserrat'] select-none">
      {/* Title Block */}
      <div className="mb-10 text-left border-b border-outline-variant pb-6">
        <h2 className="font-headline-lg text-2xl md:text-3xl text-primary uppercase italic m-0 tracking-widest font-black leading-none">
          HỒ SƠ CÁ NHÂN
        </h2>
        <p className="text-secondary text-xs uppercase tracking-widest mt-3 font-bold opacity-80">
          Quản lý thông tin tài khoản và cấu hình tùy chọn dành riêng cho hội viên
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: Membership Card */}
        <div className="lg:col-span-1 flex flex-col gap-6">
          <div className="elysian-pattern text-white rounded-none overflow-hidden shadow-xl border border-[#a20513] relative">
            <div className="bg-black/50 backdrop-blur-xs p-8 flex flex-col items-center text-center relative z-10">
              
              {/* Avatar Uploader */}
              <div className="relative group cursor-pointer mb-6" onClick={handleAvatarClick}>
                <div className="size-32 rounded-full overflow-hidden border-2 border-white shadow-md relative bg-black/40 flex items-center justify-center">
                  {avatarPreview ? (
                    <img
                      src={avatarPreview}
                      alt={profile.fullName}
                      className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                    />
                  ) : (
                    <User className="size-16 text-white/50" />
                  )}

                  {/* Upload Overlay */}
                  <div className="absolute inset-0 bg-[#a20513]/85 opacity-0 group-hover:opacity-100 transition-opacity flex flex-col items-center justify-center gap-1 text-white">
                    <Camera size={18} className="text-white" />
                    <span className="text-[9px] uppercase tracking-widest font-black">Thay ảnh</span>
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
              <h3 className="text-base font-bold font-['Playfair_Display'] text-white tracking-widest m-0 uppercase leading-normal">
                {profile.fullName || 'Hội viên Elysian'}
              </h3>
              <span className="mt-2 bg-white/10 border border-white/20 text-white px-4 py-0.5 rounded-none text-[9px] font-bold tracking-widest uppercase">
                {profile.role === 'customer' ? 'HỘI VIÊN ELITE' : (profile.role || 'CUSTOMER').toUpperCase()}
              </span>

              <div className="w-full h-px bg-white/15 my-6" />

              {/* Quick Details */}
              <div className="w-full space-y-1 text-[10px] uppercase tracking-widest font-bold">
                <div className="flex items-center justify-between py-2 border-b border-white/10">
                  <span className="text-white/60">Trạng thái</span>
                  <span className="text-white flex items-center gap-1.5">
                    <span className="size-1.5 rounded-full bg-emerald-400 animate-pulse" />
                    {profile.status === 'active' ? 'Đang hoạt động' : 'Hoạt động'}
                  </span>
                </div>
                <div className="flex items-center justify-between py-2">
                  <span className="text-white/60">Gia nhập</span>
                  <span className="text-white">
                    {formatDate(profile.createdAt)}
                  </span>
                </div>
              </div>
            </div>

            {/* Card Footer */}
            <div className="bg-[#5c030b] px-8 py-3.5 border-t border-white/10 flex justify-between items-center text-[9px]">
              <span className="text-white/70 tracking-widest font-black font-mono">ELYSIAN HOTELS & RESORTS</span>
              <Award size={14} className="text-white/80" />
            </div>
          </div>
        </div>

        {/* Right Column: Detailed Info Form */}
        <div className="lg:col-span-2">
          <div className="bg-white rounded-none p-6 md:p-8 border border-outline-variant shadow-lg text-left">
            <div className="flex justify-between items-center mb-8 border-b border-outline-variant pb-4">
              <div>
                <h3 className="text-xs font-bold uppercase tracking-widest text-primary font-['Montserrat']">
                  Thông Tin Hội Viên
                </h3>
                <p className="text-xs text-secondary mt-1.5 font-semibold opacity-85">Cập nhật hồ sơ để nhận các ưu đãi concierge cá nhân hóa</p>
              </div>

              {!isEditMode && (
                <button
                  type="button"
                  onClick={() => setIsEditMode(true)}
                  className="border border-on-surface text-on-surface font-bold text-[9px] tracking-widest uppercase px-5 py-2.5 hover:bg-slate-900 hover:text-white transition-all cursor-pointer rounded-none bg-transparent"
                >
                  Chỉnh Sửa
                </button>
              )}
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Full Name */}
                <div className="space-y-2 text-left">
                  <label className="block text-[9px] font-bold text-secondary uppercase tracking-widest flex items-center gap-1.5 select-none opacity-80">
                    <User size={13} className="text-slate-400" />
                    Họ và tên
                  </label>
                  {isEditMode ? (
                    <input
                      type="text"
                      name="fullName"
                      value={formData.fullName}
                      onChange={handleInputChange}
                      required
                      placeholder="Nhập họ và tên"
                      className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary transition-colors text-slate-800"
                    />
                  ) : (
                    <div className="border-b border-slate-200 py-2 font-bold text-sm tracking-wide text-slate-800 min-h-[38px] flex items-center">
                      {profile.fullName || 'Chưa cập nhật'}
                    </div>
                  )}
                </div>

                {/* Email (Read Only) */}
                <div className="space-y-2 text-left">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest flex items-center gap-1.5 select-none opacity-80">
                    <Mail size={13} className="text-slate-400" />
                    Địa chỉ Email
                  </label>
                  <div className="border-b border-slate-200/50 py-2 font-bold text-sm tracking-wide text-slate-500 flex items-center justify-between min-h-[38px]">
                    <span>{profile.email || 'Chưa cập nhật'}</span>
                    <span className="text-[7px] font-black text-slate-400 uppercase tracking-widest border border-slate-200 px-1.5 py-0.5 select-none">CỐ ĐỊNH</span>
                  </div>
                </div>

                {/* Phone Number (Read Only) */}
                <div className="space-y-2 text-left">
                  <label className="block text-[9px] font-bold text-slate-400 uppercase tracking-widest flex items-center gap-1.5 select-none opacity-80">
                    <Phone size={13} className="text-slate-400" />
                    Số điện thoại
                  </label>
                  <div className="border-b border-slate-200/50 py-2 font-bold text-sm tracking-wide text-slate-500 flex items-center justify-between min-h-[38px]">
                    <span>{profile.phoneNumber || 'Chưa cập nhật'}</span>
                    <span className="text-[7px] font-black text-slate-400 uppercase tracking-widest border border-slate-200 px-1.5 py-0.5 select-none">CỐ ĐỊNH</span>
                  </div>
                </div>

                {/* Address */}
                <div className="space-y-2 md:col-span-2 text-left">
                  <label className="block text-[9px] font-bold text-secondary uppercase tracking-widest flex items-center gap-1.5 select-none opacity-80">
                    <MapPin size={13} className="text-slate-400" />
                    Địa chỉ liên hệ
                  </label>
                  {isEditMode ? (
                    <input
                      type="text"
                      name="address"
                      value={formData.address}
                      onChange={handleInputChange}
                      placeholder="Nhập địa chỉ của bạn"
                      className="w-full bg-transparent border-b border-on-surface py-2 font-bold text-sm outline-none focus:border-primary transition-colors text-slate-800"
                    />
                  ) : (
                    <div className="border-b border-slate-200 py-2 font-bold text-sm tracking-wide text-slate-800 min-h-[38px] flex items-center">
                      {profile.address || 'Chưa cập nhật địa chỉ'}
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
                    Hủy bỏ
                  </button>

                  <button
                    type="submit"
                    disabled={!isDirty || isSaving}
                    className={`px-6 py-3 font-bold text-[9px] tracking-widest uppercase rounded-none flex items-center gap-1.5 transition-all cursor-pointer border-none ${
                      isDirty && !isSaving
                        ? 'bg-primary text-on-primary hover:brightness-110 shadow-md'
                        : 'bg-slate-200 text-slate-400 cursor-not-allowed'
                    }`}
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
              )}
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}
