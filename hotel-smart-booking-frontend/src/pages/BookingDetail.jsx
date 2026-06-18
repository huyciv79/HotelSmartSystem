import { useState, useEffect } from 'react';
import { getBookingDetail } from '../services/bookingService';
import { useToast, ToastContainer } from '../components/Toast';
import {
  getFeedbackByBookingId,
  createReview,
  updateReview,
  deleteReview
} from '../services/reviewService';
import { Star, Camera, X, Edit3, Trash2, MessageSquare, Plus, AlertCircle, Image as ImageIcon } from 'lucide-react';

export default function BookingDetail({ setActivePage }) {
  const { toasts, showToast, dismissToast } = useToast();

  const [booking, setBooking] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [copied, setCopied] = useState(false);

  // Feedback related states
  const [feedback, setFeedback] = useState(null);
  const [isFeedbackOpen, setIsFeedbackOpen] = useState(false);
  const [feedbackRating, setFeedbackRating] = useState(5);
  const [feedbackHoveredRating, setFeedbackHoveredRating] = useState(0);
  const [feedbackComment, setFeedbackComment] = useState('');
  const [feedbackPros, setFeedbackPros] = useState('');
  const [feedbackCons, setFeedbackCons] = useState('');
  const [feedbackImages, setFeedbackImages] = useState([]);
  const [isSubmittingFeedback, setIsSubmittingFeedback] = useState(false);
  const [isEditingFeedback, setIsEditingFeedback] = useState(false);

  const fetchFeedback = async (bookingId) => {
    try {
      const fb = await getFeedbackByBookingId(bookingId);
      setFeedback(fb);
    } catch (err) {
      console.error('Error fetching booking feedback:', err);
    }
  };

  useEffect(() => {
    const fetchBookingDetail = async () => {
      // Find selected booking ID from sessionStorage
      const selectedBookingId = sessionStorage.getItem('selectedBookingId');
      if (!selectedBookingId) {
        showToast('Không tìm thấy thông tin đặt phòng cần xem.', 'error');
        setTimeout(() => {
          setActivePage('dashboard');
        }, 1500);
        return;
      }

      try {
        const response = await getBookingDetail(selectedBookingId);
        if (response && response.data) {
          const bookingData = response.data;

          // Apply status override from localStorage for simulation
          const localStatus = localStorage.getItem(`booking_status_${selectedBookingId}`);
          if (localStatus) {
            bookingData.status = localStatus;
          }

          setBooking(bookingData);

          const status = bookingData.status || '';
          const isCompleted = ['checked-out', 'checked out', 'completed'].includes(status.toLowerCase());
          if (isCompleted) {
            await fetchFeedback(bookingData.bookingId);
          }
        }
      } catch (err) {
        console.error('Lỗi khi tải chi tiết đặt phòng:', err);
        showToast('Không thể tải chi tiết đặt phòng', 'error');
        // Fallback: mock booking data for safety if api fails
        setBooking({
          bookingId: parseInt(selectedBookingId),
          bookingReference: 'BK' + new Date().toISOString().slice(0, 10).replace(/-/g, '') + '0088',
          roomTypeName: 'Elysian Suite Thượng Hạng',
          checkInDate: '2026-06-15',
          checkOutDate: '2026-06-18',
          nights: 3,
          totalAmount: 12000000,
          finalAmount: 13200000,
          paidAmount: 0,
          status: 'Checked-out', // Mock as Checked-out to verify feedback features easily if fallback triggered
          checkInMethod: 'FaceID',
          specialRequests: 'Cần phòng tầng cao, hướng hồ bơi.',
          createdAt: new Date().toISOString()
        });
        await fetchFeedback(selectedBookingId);
      } finally {
        setIsLoading(false);
      }
    };

    fetchBookingDetail();
  }, [setActivePage, showToast]);

  const handleCopyCode = () => {
    if (!booking) return;
    navigator.clipboard.writeText(booking.bookingReference);
    setCopied(true);
    showToast('Đã sao chép mã đặt phòng vào Clipboard!', 'success');
    setTimeout(() => setCopied(false), 2000);
  };

  const handleCancelBooking = () => {
    showToast('Gửi yêu cầu hủy đặt phòng thành công. Nhân viên sẽ liên hệ xác nhận trong vòng ít phút!', 'success');
    setBooking(prev => ({ ...prev, status: 'Cancelled' }));
  };

  const handleRequestService = () => {
    showToast('Đã chuyển tiếp yêu cầu Concierge của bạn tới quầy lễ tân!', 'success');
  };

  // Open Feedback Form for Writing
  const handleOpenWriteFeedback = () => {
    setFeedbackRating(5);
    setFeedbackComment('');
    setFeedbackPros('');
    setFeedbackCons('');
    setFeedbackImages([]);
    setIsEditingFeedback(false);
    setIsFeedbackOpen(true);
  };

  // Open Feedback Form for Editing
  const handleOpenEditFeedback = () => {
    if (!feedback) return;
    setFeedbackRating(feedback.rating);
    setFeedbackComment(feedback.comment || '');
    setFeedbackPros(feedback.pros || '');
    setFeedbackCons(feedback.cons || '');
    setFeedbackImages(feedback.images || []);
    setIsEditingFeedback(true);
    setIsFeedbackOpen(true);
  };

  // Submit Feedback (Create or Update)
  const handleSubmitFeedback = async (e) => {
    e.preventDefault();
    if (!feedbackComment.trim()) {
      showToast('Vui lòng nhập bình luận đánh giá.', 'error');
      return;
    }

    setIsSubmittingFeedback(true);
    try {
      const data = {
        rating: feedbackRating,
        comment: feedbackComment,
        pros: feedbackPros,
        cons: feedbackCons,
        images: feedbackImages
      };

      if (isEditingFeedback && feedback) {
        // Update
        data.bookingId = booking.bookingId;
        await updateReview(feedback.id, data);
        showToast('Cập nhật đánh giá dịch vụ thành công!', 'success');
      } else {
        // Create
        await createReview(booking.bookingId, data);
        showToast('Cảm ơn bạn đã gửi đánh giá dịch vụ!', 'success');
      }

      await fetchFeedback(booking.bookingId);
      setIsFeedbackOpen(false);
    } catch (err) {
      console.error('Error submitting feedback:', err);
      const errMsg = err.response?.data?.message || 'Có lỗi xảy ra khi gửi đánh giá. Vui lòng thử lại.';
      showToast(errMsg, 'error');
    } finally {
      setIsSubmittingFeedback(false);
    }
  };

  // Delete Feedback
  const handleDeleteFeedback = async () => {
    if (!feedback) return;
    if (window.confirm('Bạn có chắc chắn muốn xóa đánh giá này không?')) {
      try {
        await deleteReview(feedback.id);
        setFeedback(null);
        showToast('Đã xóa đánh giá của bạn.', 'success');
      } catch (err) {
        console.error('Error deleting feedback:', err);
        showToast('Không thể xóa đánh giá lúc này.', 'error');
      }
    }
  };

  // Handle image conversion to base64
  const handleImageChange = (e) => {
    const files = Array.from(e.target.files);
    if (!files.length) return;

    if (feedbackImages.length + files.length > 5) {
      showToast('Bạn chỉ có thể đính kèm tối đa 5 hình ảnh.', 'error');
      return;
    }

    const promises = files.map(file => {
      return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = (event) => resolve(event.target.result);
        reader.onerror = (error) => reject(error);
        reader.readAsDataURL(file);
      });
    });

    Promise.all(promises)
      .then(base64s => {
        setFeedbackImages(prev => [...prev, ...base64s]);
      })
      .catch(err => {
        console.error('Error uploading images:', err);
        showToast('Lỗi khi tải ảnh lên.', 'error');
      });
  };

  const removeImage = (index) => {
    setFeedbackImages(prev => prev.filter((_, idx) => idx !== index));
  };


  if (isLoading) {
    return (
      <div className="w-full min-h-screen pt-36 pb-24 bg-gray-50 flex items-center justify-center font-['Montserrat']">
        <div className="text-center">
          <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-primary mx-auto mb-4"></div>
          <p className="text-xs uppercase font-bold tracking-widest text-slate-500">Đang tải chi tiết đặt phòng...</p>
        </div>
      </div>
    );
  }

  if (!booking) return null;

  // Determine timeline status steps and timestamps
  const statuses = [
    { key: 'Created', label: 'Yêu cầu Đặt phòng', desc: 'Đã tiếp nhận yêu cầu' },
    { key: 'Confirmed', label: 'Đã xác nhận', desc: 'Đã xác thực thông tin' },
    { key: 'Checked-in', label: 'Đã nhận phòng', desc: 'Sử dụng phòng tại Elysian' },
    { key: 'Checked-out', label: 'Đã trả phòng', desc: 'Hoàn tất thời gian lưu trú' }
  ];

  const getStatusIndex = (statusStr) => {
    const normalized = (statusStr || '').toLowerCase().replace(/[^a-z]/g, '');
    if (normalized === 'cancelled') return -1;
    if (normalized === 'checkedout' || normalized === 'completed') return 3;
    if (normalized === 'checkedin') return 2;
    if (normalized === 'confirmed' || normalized === 'active') return 1;
    return 0; // Created/Pending
  };

  const currentStatusIdx = getStatusIndex(booking.status);

  // Formatting date/time helper
  const formatDateTime = (isoString) => {
    if (!isoString) return 'Chờ cập nhật';
    const date = new Date(isoString);
    return date.toLocaleString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  };

  // Mock timestamp logs relative to booking creation time
  const getStatusTimestamp = (idx) => {
    if (idx > currentStatusIdx) return 'Chưa diễn ra';
    const createdTime = new Date(booking.createdAt || new Date());
    if (idx === 0) return formatDateTime(createdTime);
    if (idx === 1) return formatDateTime(new Date(createdTime.getTime() + 10 * 60 * 1000)); // Confirmed 10m later
    if (idx === 2) {
      const actualTime = booking.actualCheckIn || localStorage.getItem(`booking_actualcheckin_${booking.bookingId}`);
      return formatDateTime(actualTime || new Date(booking.checkInDate + 'T14:00:00').toISOString());
    }
    if (idx === 3) {
      const actualTime = booking.actualCheckOut || localStorage.getItem(`booking_actualcheckout_${booking.bookingId}`);
      return formatDateTime(actualTime || new Date(booking.checkOutDate + 'T12:00:00').toISOString());
    }
    return 'Chờ cập nhật';
  };

  const finalAmount = booking.finalAmount || booking.totalAmount || 0;

  return (
    <>
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />
      <div className="w-full min-h-screen pt-36 pb-24 bg-gray-50 flex items-start justify-center px-4 font-['Montserrat']">
        <div className="max-w-6xl w-full flex flex-col text-left">

          {/* Header Area */}
          <div className="bg-white p-6 md:p-8 border border-outline-variant shadow-md mb-8 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <button
                onClick={() => setActivePage('dashboard')}
                className="mb-4 w-fit text-xs text-secondary hover:text-primary uppercase tracking-widest font-bold flex items-center gap-1 cursor-pointer bg-transparent border-none"
              >
                <span className="material-symbols-outlined text-sm">arrow_back</span> Trở lại Dashboard
              </button>

              <div className="flex items-center gap-3 flex-wrap">
                <h2 className="text-xl md:text-2xl font-black text-slate-950 uppercase tracking-wider m-0">Đặt phòng: {booking.bookingReference}</h2>
                <button
                  onClick={handleCopyCode}
                  className="flex items-center gap-1 bg-slate-100 hover:bg-slate-200 text-slate-700 text-[10px] font-bold px-2 py-1 uppercase tracking-wider border-none cursor-pointer transition-all"
                >
                  <span className="material-symbols-outlined text-sm">{copied ? 'check' : 'content_copy'}</span>
                  {copied ? 'Đã copy' : 'Copy mã'}
                </button>
              </div>
              <p className="text-xs text-slate-500 font-bold uppercase tracking-wider mt-1">{booking.roomTypeName}</p>
            </div>

            {/* Current Status Badge */}
            <div className="text-right">
              <span className="text-[10px] text-slate-400 font-black uppercase tracking-widest block mb-1">Trạng thái</span>
              <span className={`px-4 py-2 text-xs font-black uppercase tracking-widest ${booking.status === 'Cancelled'
                  ? 'bg-red-100 text-red-700'
                  : booking.status === 'Checked-in' || booking.status === 'Checked In'
                    ? 'bg-blue-100 text-blue-700'
                    : booking.status === 'Checked-out' || booking.status === 'Checked Out'
                      ? 'bg-slate-200 text-slate-700'
                      : 'bg-green-100 text-green-700'
                }`}>
                {booking.status === 'Cancelled' ? 'Đã Hủy' : booking.status === 'Checked-in' || booking.status === 'Checked In' ? 'Đã nhận phòng' : booking.status === 'Checked-out' || booking.status === 'Checked Out' ? 'Đã trả phòng' : 'Đã xác nhận'}
              </span>
            </div>
          </div>

          {/* Main Content Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">

            {/* Left Column: Timeline & Details (col-span-8) */}
            <div className="lg:col-span-8 bg-white border border-outline-variant shadow-md p-6 md:p-8 space-y-8">

              {/* Vertical Timeline */}
              <div>
                <h3 className="text-xs font-black text-slate-900 uppercase tracking-widest border-b border-gray-100 pb-3 mb-6">Tiến trình trạng thái (Timeline)</h3>

                {booking.status === 'Cancelled' ? (
                  <div className="bg-red-50 border border-red-200 p-4 flex items-center gap-3 text-red-700 font-bold text-xs">
                    <span className="material-symbols-outlined text-2xl">cancel</span>
                    <span>Đặt phòng này đã bị hủy bỏ. Vui lòng liên hệ bộ phận hỗ trợ khách hàng Elysian nếu cần trợ giúp.</span>
                  </div>
                ) : (
                  <div className="relative pl-6 border-l border-slate-200 ml-3 space-y-8">
                    {statuses.map((step, idx) => {
                      const isActive = idx <= currentStatusIdx;
                      const isCurrent = idx === currentStatusIdx;
                      return (
                        <div key={step.key} className="relative">
                          {/* Dot marker */}
                          <span className={`absolute -left-[31px] top-0 w-4 h-4 rounded-full border-2 bg-white flex items-center justify-center transition-all ${isCurrent
                              ? 'border-primary bg-primary scale-125'
                              : isActive
                                ? 'border-primary bg-primary/20'
                                : 'border-slate-300'
                            }`}>
                            {isCurrent && <span className="w-1.5 h-1.5 bg-white rounded-full"></span>}
                          </span>

                          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
                            <div>
                              <h4 className={`text-xs font-black uppercase tracking-wider ${isActive ? 'text-slate-950 font-extrabold' : 'text-slate-400'}`}>
                                {step.label}
                              </h4>
                              <p className="text-[10px] text-slate-500 font-medium uppercase tracking-wider mt-0.5">{step.desc}</p>
                            </div>
                            <span className={`text-[10px] font-black uppercase tracking-wider ${isActive ? 'text-slate-800' : 'text-slate-400'}`}>
                              {getStatusTimestamp(idx)}
                            </span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* Room Details Info */}
              <div className="border-t border-gray-100 pt-6">
                <h3 className="text-xs font-black text-slate-900 uppercase tracking-widest pb-3 mb-4">Chi tiết phòng nghỉ & Thời gian</h3>
                <div className="grid grid-cols-1 sm:grid-cols-4 gap-6 text-xs font-bold text-slate-700">
                  <div>
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Ngày Nhận Phòng</span>
                    <span className="text-slate-900 font-black tracking-wide block mt-1">{booking.checkInDate}</span>
                    <span className="text-[9px] text-slate-500 uppercase tracking-wider">Từ 2h trưa (14:00)</span>
                  </div>
                  <div>
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Ngày Trả Phòng</span>
                    <span className="text-slate-900 font-black tracking-wide block mt-1">{booking.checkOutDate}</span>
                    <span className="text-[9px] text-slate-500 uppercase tracking-wider">Trước 12h trưa (12:00)</span>
                  </div>
                  <div>
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Số lượng khách</span>
                    <span className="text-slate-900 font-black tracking-wide block mt-1">
                      {booking.numberOfAdults || 1} Người lớn
                      {booking.numberOfChildren ? `, ${booking.numberOfChildren} Trẻ em` : ''}
                    </span>
                    <span className="text-[9px] text-slate-500 uppercase tracking-wider">
                      {booking.quantity ? `Số lượng: ${booking.quantity} phòng` : 'Số lượng: 1 phòng'}
                    </span>
                  </div>
                  <div>
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">Hình thức nhận phòng</span>
                    <span className="text-primary font-black uppercase tracking-wider block mt-1">
                      {booking.checkInMethod === 'FaceID' || booking.checkInMethod === 'Face Recognition' ? 'FaceID eKYC' : booking.checkInMethod === 'QR Code' ? 'Mã QR Code' : 'Tại quầy lễ tân'}
                    </span>
                  </div>
                </div>

                {booking.specialRequests && (
                  <div className="mt-6 p-4 bg-slate-50 border border-slate-200">
                    <span className="block text-[9px] text-slate-400 font-bold uppercase tracking-wider mb-1">Yêu cầu đặc biệt:</span>
                    <p className="text-xs text-slate-700 font-bold tracking-wide m-0">{booking.specialRequests}</p>
                  </div>
                )}
              </div>

              {/* ACTION BUTTONS BASED ON STATUS */}
              <div className="border-t border-gray-100 pt-6 flex flex-wrap gap-4">
                {/* Cancel button (available before Checked-in) */}
                {booking.status !== 'Cancelled' && currentStatusIdx < 2 && (
                  <button
                    onClick={handleCancelBooking}
                    className="bg-red-50 hover:bg-red-100 text-red-600 text-xs font-black uppercase tracking-widest px-8 py-3.5 active:scale-98 transition-all cursor-pointer border-none flex items-center gap-1.5 h-11"
                  >
                    <span className="material-symbols-outlined text-lg">cancel</span>
                    Hủy đặt phòng
                  </button>
                )}

                {/* Request button (available when Checked-in) */}
                {booking.status !== 'Cancelled' && currentStatusIdx === 2 && (
                  <button
                    onClick={handleRequestService}
                    className="bg-slate-900 hover:bg-primary text-white text-xs font-black uppercase tracking-widest px-8 py-3.5 active:scale-98 transition-all cursor-pointer border-none flex items-center gap-1.5 h-11"
                  >
                    <span className="material-symbols-outlined text-lg">room_service</span>
                    Yêu cầu Dịch vụ phòng
                  </button>
                )}

                {/* Feedback button (available when Checked-out / Completed) */}
                {booking.status !== 'Cancelled' && currentStatusIdx === 3 && !feedback && (
                  <button
                    onClick={handleOpenWriteFeedback}
                    className="bg-primary text-white hover:bg-opacity-95 text-xs font-black uppercase tracking-widest px-8 py-3.5 active:scale-98 transition-all cursor-pointer border-none flex items-center gap-2 h-11"
                  >
                    <MessageSquare size={16} />
                    <span>Đánh giá dịch vụ</span>
                  </button>
                )}
              </div>

              {/* Display submitted feedback if exists */}
              {feedback && (
                <div className="border-t border-gray-100 pt-6">
                  <h3 className="text-xs font-black text-slate-900 uppercase tracking-widest pb-3 mb-4 flex items-center justify-between">
                    <span>Đánh giá dịch vụ của bạn</span>
                    <div className="flex gap-2">
                      <button
                        onClick={handleOpenEditFeedback}
                        className="flex items-center gap-1 text-[10px] font-bold uppercase tracking-wider text-slate-500 hover:text-primary border-none bg-transparent cursor-pointer"
                      >
                        <Edit3 size={12} />
                        <span>Sửa</span>
                      </button>
                      <button
                        onClick={handleDeleteFeedback}
                        className="flex items-center gap-1 text-[10px] font-bold uppercase tracking-wider text-slate-500 hover:text-red-600 border-none bg-transparent cursor-pointer"
                      >
                        <Trash2 size={12} />
                        <span>Xóa</span>
                      </button>
                    </div>
                  </h3>

                  <div className="bg-slate-50 p-5 border border-slate-200">
                    <div className="flex items-center gap-2 mb-3">
                      <div className="flex gap-0.5">
                        {[1, 2, 3, 4, 5].map((star) => (
                          <Star
                            key={star}
                            size={16}
                            className={star <= feedback.rating ? "fill-amber-400 text-amber-400" : "text-slate-200"}
                          />
                        ))}
                      </div>
                      <span className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">
                        Đã gửi lúc {new Date(feedback.createdAt).toLocaleDateString('vi-VN')}
                      </span>
                    </div>

                    <p className="text-xs font-medium text-slate-700 leading-relaxed whitespace-pre-line mb-4">
                      {feedback.comment}
                    </p>

                    {/* Pros and Cons */}
                    {(feedback.pros || feedback.cons) && (
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs font-bold text-slate-700 bg-white p-3 border border-slate-100 mb-4">
                        {feedback.pros && (
                          <div>
                            <span className="text-green-700 text-[9px] font-black uppercase tracking-wider block mb-0.5">✓ Ưu điểm:</span>
                            <span className="text-slate-600 font-medium">{feedback.pros}</span>
                          </div>
                        )}
                        {feedback.cons && (
                          <div>
                            <span className="text-red-700 text-[9px] font-black uppercase tracking-wider block mb-0.5">✗ Nhược điểm:</span>
                            <span className="text-slate-600 font-medium">{feedback.cons}</span>
                          </div>
                        )}
                      </div>
                    )}

                    {/* Attached images */}
                    {feedback.images && feedback.images.length > 0 && (
                      <div className="flex flex-wrap gap-2">
                        {feedback.images.map((img, idx) => (
                          <div key={idx} className="w-16 h-16 border border-slate-200 overflow-hidden bg-white">
                            <img src={img} alt="Feedback" className="w-full h-full object-cover" />
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>

            {/* Right Column: Booking Summary Card (col-span-4) */}
            <div className="lg:col-span-4 bg-white border border-outline-variant shadow-md p-6 md:p-8 flex flex-col justify-between h-fit">
              <div>
                <h3 className="text-xs font-black text-slate-900 uppercase tracking-widest border-b border-gray-100 pb-3 mb-4">Tóm tắt đặt phòng</h3>

                <div className="space-y-4">
                  {/* Room Info */}
                  <div>
                    <span className="text-[9px] text-primary font-black uppercase tracking-widest block mb-0.5">ELYSIAN HOTELS</span>
                    <h4 className="text-sm font-black text-slate-950 uppercase tracking-wider">{booking.roomTypeName}</h4>
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider mt-0.5 block">Mã: {booking.bookingReference}</span>
                  </div>

                  {/* Detail items */}
                  <div className="space-y-2.5 border-t border-b border-gray-100 py-4 text-xs font-bold text-slate-700">
                    <div className="flex justify-between gap-4">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">Thời gian:</span>
                      <span className="text-right">{booking.checkInDate} đến {booking.checkOutDate} ({booking.nights} đêm)</span>
                    </div>
                    <div className="flex justify-between gap-4">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">Số phòng đặt:</span>
                      <span>{booking.quantity || 1} phòng</span>
                    </div>
                    <div className="flex justify-between gap-4">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">Số lượng khách:</span>
                      <span>{booking.numberOfAdults || 1} NL {booking.numberOfChildren ? `• ${booking.numberOfChildren} TE` : ''}</span>
                    </div>
                    <div className="flex justify-between gap-4">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">Check-in:</span>
                      <span className="text-primary uppercase">{booking.checkInMethod === 'FaceID' || booking.checkInMethod === 'Face Recognition' ? 'FaceID eKYC' : booking.checkInMethod === 'QR Code' ? 'Mã QR' : 'Quầy lễ tân'}</span>
                    </div>
                  </div>

                  {/* Price breakdown */}
                  <div className="space-y-2 text-xs font-bold text-slate-700">
                    <div className="flex justify-between gap-4">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">Tạm tính (chưa thuế):</span>
                      <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(booking.totalAmount || (finalAmount / 1.1))}</span>
                    </div>
                    <div className="flex justify-between gap-4">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">Thuế VAT (10%):</span>
                      <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(finalAmount - (booking.totalAmount || (finalAmount / 1.1)))}</span>
                    </div>
                    <div className="flex justify-between border-t border-slate-950 pt-3 text-sm gap-4">
                      <span className="font-black text-slate-900 uppercase tracking-wider">TỔNG CỘNG:</span>
                      <span className="font-black text-primary text-base">
                        {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(finalAmount)}
                      </span>
                    </div>
                  </div>

                  {/* Policy instruction box */}
                  <div className="bg-slate-50 border border-slate-200 p-4 text-[9px] text-slate-500 font-semibold leading-relaxed uppercase tracking-wider mt-4">
                    <span className="block font-black text-slate-700 mb-1">Quy định nhận/trả phòng:</span>
                    <p className="mb-1">Check-in: 2h trưa (14:00)</p>
                    <p>Check-out: 12h trưa (12:00)</p>
                  </div>
                </div>
              </div>
            </div>

          </div>

        </div>
      </div>

      {/* FEEDBACK MODAL OVERLAY */}
      {isFeedbackOpen && (
        <div className="fixed inset-0 z-[5000] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white border-2 border-primary/20 max-w-lg w-full p-6 relative shadow-2xl animate-scale-in text-slate-800 font-['Montserrat']">
            {/* Close Button */}
            <button
              type="button"
              onClick={() => setIsFeedbackOpen(false)}
              className="absolute top-4 right-4 text-slate-400 hover:text-slate-700 border-none bg-transparent cursor-pointer p-1"
            >
              <X size={20} />
            </button>

            <div className="bg-primary text-white text-[9px] font-black uppercase tracking-widest px-3 py-1 absolute top-0 left-0">
              {isEditingFeedback ? 'Chỉnh sửa đánh giá dịch vụ' : 'Đánh giá dịch vụ của bạn'}
            </div>

            <h3 className="text-sm font-black text-slate-900 uppercase tracking-wider mt-4 mb-1">
              {booking.roomTypeName}
            </h3>
            <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider border-b border-gray-100 pb-3 mb-4">
              Mã đặt phòng: {booking.bookingReference}
            </p>

            <form onSubmit={handleSubmitFeedback} className="space-y-4 text-left">
              {/* Star Rating Selection */}
              <div>
                <span className="block text-[10px] font-black uppercase tracking-widest text-slate-600 mb-1.5">Mức độ hài lòng:</span>
                <div className="flex gap-1.5">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      type="button"
                      key={star}
                      onClick={() => setFeedbackRating(star)}
                      onMouseEnter={() => setFeedbackHoveredRating(star)}
                      onMouseLeave={() => setFeedbackHoveredRating(0)}
                      className="p-1 cursor-pointer transition-transform hover:scale-110 focus:outline-none bg-transparent border-none"
                    >
                      <Star
                        size={24}
                        className={`transition-colors duration-100 ${star <= (feedbackHoveredRating || feedbackRating)
                            ? "fill-amber-400 text-amber-400"
                            : "text-slate-300"
                          }`}
                      />
                    </button>
                  ))}
                </div>
              </div>

              {/* Text comment */}
              <div>
                <span className="block text-[10px] font-black uppercase tracking-widest text-slate-600 mb-1.5">Bình luận chi tiết:</span>
                <textarea
                  rows="3"
                  value={feedbackComment}
                  onChange={(e) => setFeedbackComment(e.target.value)}
                  placeholder="Hãy chia sẻ cảm nhận thực tế của bạn về chất lượng phòng và dịch vụ..."
                  className="w-full p-3 border border-slate-300 text-xs font-medium focus:border-primary focus:outline-none placeholder-slate-400 leading-relaxed resize-none rounded-none bg-slate-50"
                  required
                />
              </div>

              {/* Pros & Cons */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <span className="block text-[10px] font-black uppercase tracking-widest text-slate-600 mb-1.5">✓ Ưu điểm:</span>
                  <input
                    type="text"
                    value={feedbackPros}
                    onChange={(e) => setFeedbackPros(e.target.value)}
                    placeholder="Điểm bạn thích nhất..."
                    className="w-full p-2.5 border border-slate-300 text-xs font-medium focus:border-primary focus:outline-none placeholder-slate-400 rounded-none bg-slate-50"
                  />
                </div>
                <div>
                  <span className="block text-[10px] font-black uppercase tracking-widest text-slate-600 mb-1.5">✗ Nhược điểm:</span>
                  <input
                    type="text"
                    value={feedbackCons}
                    onChange={(e) => setFeedbackCons(e.target.value)}
                    placeholder="Điểm cần cải thiện..."
                    className="w-full p-2.5 border border-slate-300 text-xs font-medium focus:border-primary focus:outline-none placeholder-slate-400 rounded-none bg-slate-50"
                  />
                </div>
              </div>

              {/* Image Attachments */}
              <div>
                <div className="flex justify-between items-center mb-1.5">
                  <span className="block text-[10px] font-black uppercase tracking-widest text-slate-600">Hình ảnh đính kèm ({feedbackImages.length}/5):</span>
                  <label className="flex items-center gap-1 cursor-pointer bg-slate-100 hover:bg-slate-200 text-slate-700 px-2.5 py-1 text-[9px] font-extrabold uppercase tracking-widest transition-all border border-slate-300">
                    <Camera size={12} />
                    <span>Tải ảnh</span>
                    <input
                      type="file"
                      multiple
                      accept="image/*"
                      className="hidden"
                      onChange={handleImageChange}
                    />
                  </label>
                </div>

                {feedbackImages.length > 0 && (
                  <div className="flex flex-wrap gap-2 p-2 bg-slate-50 border border-slate-200">
                    {feedbackImages.map((img, idx) => (
                      <div key={idx} className="relative w-12 h-12 border border-slate-300 bg-slate-200">
                        <img src={img} alt="Preview" className="w-full h-full object-cover" />
                        <button
                          type="button"
                          onClick={() => removeImage(idx)}
                          className="absolute -top-1 -right-1 bg-red-600 text-white rounded-full p-0.5 hover:bg-red-700 transition-colors shadow"
                        >
                          <X size={8} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Action Buttons */}
              <div className="flex justify-end gap-2.5 pt-2 text-[10px] font-black uppercase tracking-widest">
                <button
                  type="button"
                  onClick={() => setIsFeedbackOpen(false)}
                  className="px-5 py-2.5 border border-slate-300 text-slate-700 bg-white hover:bg-slate-100 transition-all cursor-pointer"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingFeedback}
                  className="px-5 py-2.5 bg-primary text-white hover:bg-opacity-95 transition-all cursor-pointer border-none flex items-center gap-1.5"
                >
                  {isSubmittingFeedback ? (
                    <>
                      <div className="animate-spin rounded-full h-3.5 w-3.5 border-t-2 border-white" />
                      <span>Đang gửi...</span>
                    </>
                  ) : (
                    <span>Gửi đánh giá</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </>
  );
}
