import { useState, useEffect } from 'react';
import { generateQrCheckInToken, getBookingDetail, customerCancelBooking } from '../services/bookingService';
import { submitRefundRequest } from '../services/refundService';
import { submitCustomerRoomChangeRequest } from '../services/roomChangeService';
import { submitStayExtensionRequest } from '../services/stayExtensionService';
import { submitEarlyCheckOutRequest } from '../services/earlyCheckOutService';
import {
  getServices,
  submitCustomerServiceRequest,
  getServiceRequestsByBooking
} from '../services/serviceService';
import { getRoomTypes } from '../services/roomService';
import { useToast, ToastContainer } from '../components/Toast';
import QrCheckInCard from '../components/booking/QrCheckInCard';
import {
  getFeedbackByBookingId,
  createReview,
  updateReview,
  deleteReview
} from '../services/reviewService';
import { Star, Camera, X, Edit3, Trash2, MessageSquare, Plus, AlertCircle, Image as ImageIcon } from 'lucide-react';
import { useLanguage } from '../context/LanguageContext';

export default function BookingDetail({ setActivePage }) {
  const { t } = useLanguage();
  const { toasts, showToast, dismissToast } = useToast();

  const [booking, setBooking] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [copied, setCopied] = useState(false);
  const [qrTokenData, setQrTokenData] = useState(null);
  const [isGeneratingQr, setIsGeneratingQr] = useState(false);
  const [copiedQr, setCopiedQr] = useState(false);

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

  // Refund related states
  const [isRefundModalOpen, setIsRefundModalOpen] = useState(false);
  const [refundReason, setRefundReason] = useState('');
  const [isSubmittingRefund, setIsSubmittingRefund] = useState(false);

  // Room Change related states
  const [isRoomChangeModalOpen, setIsRoomChangeModalOpen] = useState(false);
  const [roomChangeReason, setRoomChangeReason] = useState('');
  const [roomChangeOption, setRoomChangeOption] = useState('same_type');
  const [isRoomChangePending, setIsRoomChangePending] = useState(false);
  const [isSubmittingRoomChange, setIsSubmittingRoomChange] = useState(false);
  const [availableRoomTypes, setAvailableRoomTypes] = useState([]);
  const [selectedRoomType, setSelectedRoomType] = useState(null);
  const [isLoadingRooms, setIsLoadingRooms] = useState(false);
  const [roomChangeError, setRoomChangeError] = useState(null);

  // Stay Extension related states
  const [isStayExtensionModalOpen, setIsStayExtensionModalOpen] = useState(false);
  const [newCheckOutDate, setNewCheckOutDate] = useState('');
  const [extensionReason, setExtensionReason] = useState('');
  const [isStayExtensionPending, setIsStayExtensionPending] = useState(false);
  const [isSubmittingExtension, setIsSubmittingExtension] = useState(false);

  // Early Check-out related states
  const [isEarlyCheckOutModalOpen, setIsEarlyCheckOutModalOpen] = useState(false);
  const [newEarlyCheckOutDate, setNewEarlyCheckOutDate] = useState('');
  const [earlyCheckOutReason, setEarlyCheckOutReason] = useState('');
  const [isEarlyCheckOutPending, setIsEarlyCheckOutPending] = useState(false);
  const [isSubmittingEarlyCheckOut, setIsSubmittingEarlyCheckOut] = useState(false);

  // Cancellation confirm states
  const [isCancelConfirmOpen, setIsCancelConfirmOpen] = useState(false);
  const [isSubmittingCancel, setIsSubmittingCancel] = useState(false);

  // In-Stay Service Request states
  const [isServiceModalOpen, setIsServiceModalOpen] = useState(false);
  const [availableServices, setAvailableServices] = useState([]);
  const [selectedServiceId, setSelectedServiceId] = useState('');
  const [serviceQuantity, setServiceQuantity] = useState(1);
  const [serviceNote, setServiceNote] = useState('');
  const [isSubmittingServiceRequest, setIsSubmittingServiceRequest] = useState(false);
  const [myServiceRequests, setMyServiceRequests] = useState([]);
  const [isLoadingServices, setIsLoadingServices] = useState(false);

  const fetchServicesAndMyRequests = async () => {
    if (!booking?.bookingId) return;
    setIsLoadingServices(true);
    try {
      const [servicesRes, requestsRes] = await Promise.all([
        getServices(),
        getServiceRequestsByBooking(booking.bookingId)
      ]);
      setAvailableServices(servicesRes?.data || []);
      setMyServiceRequests(requestsRes?.data || []);
    } catch (err) {
      console.error('Lỗi khi tải dịch vụ phòng:', err);
    } finally {
      setIsLoadingServices(false);
    }
  };

  const handleOpenServiceModal = async () => {
    setIsServiceModalOpen(true);
    await fetchServicesAndMyRequests();
  };

  const handleSubmitServiceRequest = async (e) => {
    e.preventDefault();
    if (!selectedServiceId) {
      showToast('Vui lòng chọn dịch vụ đi kèm', 'warning');
      return;
    }
    if (serviceQuantity < 1) {
      showToast('Số lượng phải lớn hơn 0', 'warning');
      return;
    }

    setIsSubmittingServiceRequest(true);
    try {
      const payload = {
        bookingId: booking.bookingId,
        serviceId: parseInt(selectedServiceId),
        quantity: parseInt(serviceQuantity),
        note: serviceNote
      };
      const res = await submitCustomerServiceRequest(payload);
      if (res && res.success) {
        showToast('Đã gửi yêu cầu dịch vụ thành công! Vui lòng chờ nhân viên tiếp nhận.', 'success');
        setSelectedServiceId('');
        setServiceQuantity(1);
        setServiceNote('');
        await fetchServicesAndMyRequests();
      }
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Không thể gửi yêu cầu dịch vụ lúc này.', 'error');
    } finally {
      setIsSubmittingServiceRequest(false);
    }
  };

  const handleRefundSubmit = async (e) => {
    e.preventDefault();
    if (!refundReason.trim()) {
      showToast(t('Vui lòng nhập lý do hoàn tiền'), 'warning');
      return;
    }
    setIsSubmittingRefund(true);
    try {
      const response = await submitRefundRequest(booking.bookingId, refundReason);
      if (response && response.success) {
        showToast(t('Đã gửi yêu cầu hoàn tiền thành công! Yêu cầu đang chờ duyệt.'), 'success');
        setIsRefundModalOpen(false);
        setRefundReason('');
        setBooking(prev => ({ ...prev, status: 'Refund Pending' }));
      }
    } catch (err) {
      console.error(err);
      showToast(t(err.response?.data?.message, t('Lỗi khi gửi yêu cầu hoàn tiền.')), 'error');
    } finally {
      setIsSubmittingRefund(false);
    }
  };

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
        showToast(t('bd_toast_error_load', 'Không tìm thấy thông tin đặt phòng cần xem.'), 'error');
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

          // Read pending flags directly from booking API response
          setIsRoomChangePending(bookingData.isRoomChangePending === true);
          setIsStayExtensionPending(bookingData.isStayExtensionPending === true);
          setIsEarlyCheckOutPending(bookingData.isEarlyCheckOutPending === true);
        }
      } catch (err) {
        console.error('Lỗi khi tải chi tiết đặt phòng:', err);
        showToast(t('bd_toast_error_load', 'Không thể tải chi tiết đặt phòng'), 'error');
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
  }, [setActivePage, showToast, t]);

  const handleCopyCode = () => {
    if (!booking) return;
    navigator.clipboard.writeText(booking.bookingReference);
    setCopied(true);
    showToast(t('bd_btn_copied', 'Đã sao chép mã đặt phòng vào Clipboard!'), 'success');
    setTimeout(() => setCopied(false), 2000);
  };

  const handleGenerateQrToken = async () => {
    if (!booking?.bookingId) return;

    setIsGeneratingQr(true);
    try {
      const response = await generateQrCheckInToken(booking.bookingId);
      setQrTokenData(response?.data || null);
      showToast('Đã tạo mã QR check-in. Mã chỉ có hiệu lực ngắn hạn.', 'success');
    } catch (err) {
      console.error('Error generating QR check-in token:', err);
      showToast(err.response?.data?.message || 'Không thể tạo mã QR check-in lúc này.', 'error');
    } finally {
      setIsGeneratingQr(false);
    }
  };

  const handleCopyQrToken = async () => {
    const value = qrTokenData?.qrPayload || qrTokenData?.token;
    if (!value) return;

    try {
      await navigator.clipboard.writeText(value);
      setCopiedQr(true);
      showToast('Đã sao chép mã QR check-in.', 'success');
      setTimeout(() => setCopiedQr(false), 2000);
    } catch (err) {
      console.error('Error copying QR token:', err);
      showToast('Không thể sao chép mã QR trên trình duyệt này.', 'error');
    }
  };

  // Room Change fetch & submit
  const fetchRoomTypesForChange = async () => {
    setIsLoadingRooms(true);
    setRoomChangeError(null);
    try {
      const res = await getRoomTypes('Active');
      if (res && res.success) {
        const list = res.data?.content || res.data || [];
        setAvailableRoomTypes(list);
      }
    } catch (err) {
      console.error('Lỗi khi tải danh sách hạng phòng:', err);
      setRoomChangeError('Không thể tải danh sách hạng phòng.');
    } finally {
      setIsLoadingRooms(false);
    }
  };

  useEffect(() => {
    if (isRoomChangeModalOpen && roomChangeOption === 'different_type') {
      fetchRoomTypesForChange();
    }
  }, [isRoomChangeModalOpen, roomChangeOption]);

  const handleCustomerRoomChangeSubmit = async (e) => {
    e.preventDefault();
    if (roomChangeOption === 'different_type' && !selectedRoomType) {
      showToast(t('Vui lòng chọn hạng phòng mong muốn'), 'warning');
      return;
    }

    const targetRoomTypeId = roomChangeOption === 'same_type' ? booking.roomTypeId : selectedRoomType.id;

    setIsSubmittingRoomChange(true);
    setRoomChangeError(null);
    try {
      const payload = {
        bookingId: booking.bookingId,
        newRoomId: targetRoomTypeId,
        reason: roomChangeReason.trim() || undefined
      };
      const response = await submitCustomerRoomChangeRequest(payload);
      if (response && response.success) {
        showToast(t('Gửi yêu cầu chuyển phòng thành công! Quản lý sẽ sớm phê duyệt.'), 'success');
        setIsRoomChangeModalOpen(false);
        setIsRoomChangePending(true);
      }
    } catch (err) {
      console.error(err);
      setRoomChangeError(t(err.response?.data?.message, t('Gửi yêu cầu chuyển phòng thất bại. Vui lòng thử lại.')));
    } finally {
      setIsSubmittingRoomChange(false);
    }
  };

  // Stay Extension submit
  const handleStayExtensionSubmit = async (e) => {
    e.preventDefault();
    if (!newCheckOutDate) {
      showToast(t('Vui lòng chọn ngày trả phòng mới'), 'warning');
      return;
    }
    setIsSubmittingExtension(true);
    try {
      const response = await submitStayExtensionRequest({
        bookingId: booking.bookingId,
        newCheckOutDate,
        description: extensionReason.trim() || 'Khách yêu cầu gia hạn lưu trú'
      });
      if (response && response.success) {
        showToast(t('Gửi yêu cầu gia hạn lưu trú thành công!'), 'success');
        setIsStayExtensionPending(true);
        setIsStayExtensionModalOpen(false);
      }
    } catch (err) {
      console.error('Lỗi khi gửi yêu cầu gia hạn:', err);
      showToast(t(err.response?.data?.message, t('Không thể gửi yêu cầu gia hạn lưu trú.')), 'error');
    } finally {
      setIsSubmittingExtension(false);
    }
  };

  // Early Check-out submit
  const handleEarlyCheckOutSubmit = async (e) => {
    e.preventDefault();
    if (!newEarlyCheckOutDate) {
      showToast(t('Vui lòng chọn ngày trả phòng mới'), 'warning');
      return;
    }
    setIsSubmittingEarlyCheckOut(true);
    try {
      const response = await submitEarlyCheckOutRequest({
        bookingId: booking.bookingId,
        newCheckOutDate: newEarlyCheckOutDate,
        description: earlyCheckOutReason.trim() || 'Khách yêu cầu check-out sớm'
      });
      if (response && response.success) {
        showToast(t('Gửi yêu cầu check-out sớm thành công!'), 'success');
        setIsEarlyCheckOutPending(true);
        setIsEarlyCheckOutModalOpen(false);
      }
    } catch (err) {
      console.error('Lỗi khi gửi yêu cầu check-out sớm:', err);
      showToast(t(err.response?.data?.message, t('Không thể gửi yêu cầu check-out sớm.')), 'error');
    } finally {
      setIsSubmittingEarlyCheckOut(false);
    }
  };

  const handleCancelBooking = () => {
    setIsCancelConfirmOpen(true);
  };

  const confirmCancelBooking = async () => {
    setIsSubmittingCancel(true);
    try {
      const response = await customerCancelBooking(booking.bookingId, 'Khách hàng tự hủy trực tuyến');
      if (response && response.success) {
        showToast(t('bd_toast_cancel_success', 'Hủy đơn đặt phòng thành công!'), 'success');
        setBooking(prev => ({ ...prev, status: 'Cancelled' }));
        setIsCancelConfirmOpen(false);
      }
    } catch (err) {
      console.error('Lỗi khi hủy đặt phòng:', err);
      showToast(t(err.response?.data?.message, t('bd_toast_cancel_error', 'Hủy đặt phòng thất bại.')), 'error');
    } finally {
      setIsSubmittingCancel(false);
    }
  };

  const handleRequestService = () => {
    showToast(t('bd_toast_service_success', 'Đã chuyển tiếp yêu cầu Concierge của bạn tới quầy lễ tân!'), 'success');
  };

  const handlePaymentRedirect = () => {
    if (!booking) return;
    sessionStorage.setItem('selectedBookingId', booking.bookingId);
    sessionStorage.setItem('currentBooking', JSON.stringify(booking));
    setActivePage('payment');
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
      showToast(t('bd_toast_comment_required', 'Vui lòng nhập bình luận đánh giá.'), 'warning');
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
        showToast(t('bd_toast_fb_update_success', 'Cập nhật đánh giá dịch vụ thành công!'), 'success');
      } else {
        // Create
        await createReview(booking.bookingId, data);
        showToast(t('bd_toast_fb_create_success', 'Cảm ơn bạn đã gửi đánh giá dịch vụ!'), 'success');
      }

      await fetchFeedback(booking.bookingId);
      setIsFeedbackOpen(false);
    } catch (err) {
      console.error('Error submitting feedback:', err);
      const errMsg = t(err.response?.data?.message, t('bd_toast_error_submit', 'Có lỗi xảy ra khi gửi đánh giá. Vui lòng thử lại.'));
      showToast(errMsg, 'error');
    } finally {
      setIsSubmittingFeedback(false);
    }
  };

  // Delete Feedback
  const handleDeleteFeedback = async () => {
    if (!feedback) return;
    if (window.confirm(t('bd_toast_fb_delete_confirm', 'Bạn có chắc chắn muốn xóa đánh giá này không?'))) {
      try {
        await deleteReview(feedback.id);
        setFeedback(null);
        showToast(t('bd_toast_fb_delete_success', 'Đã xóa đánh giá của bạn.'), 'success');
      } catch (err) {
        console.error('Error deleting feedback:', err);
        showToast(t('bd_toast_fb_delete_error', 'Không thể xóa đánh giá lúc này.'), 'error');
      }
    }
  };

  // Handle image conversion to base64
  const handleImageChange = (e) => {
    const files = Array.from(e.target.files);
    if (!files.length) return;

    if (feedbackImages.length + files.length > 5) {
      showToast(t('bd_toast_images_limit', 'Bạn chỉ có thể đính kèm tối đa 5 hình ảnh.'), 'warning');
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
        showToast(t('bd_toast_images_error', 'Lỗi khi tải ảnh lên.'), 'error');
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
          <p className="text-xs uppercase font-bold tracking-widest text-slate-500">{t('bd_loading_text', 'Đang tải chi tiết đặt phòng...')}</p>
        </div>
      </div>
    );
  }

  if (!booking) return null;

  // Determine timeline status steps and timestamps
  const statuses = [
    { key: 'Created', label: t('bd_timeline_step0_title', 'Yêu cầu Đặt phòng'), desc: t('bd_timeline_step0_desc', 'Đã tiếp nhận yêu cầu') },
    { key: 'Confirmed', label: t('bd_timeline_step1_title', 'Đã xác nhận'), desc: t('bd_timeline_step1_desc', 'Đã xác thực thông tin') },
    { key: 'Checked-in', label: t('bd_timeline_step2_title', 'Đã nhận phòng'), desc: t('bd_timeline_step2_desc', 'Sử dụng phòng tại Elysian') },
    { key: 'Checked-out', label: t('bd_timeline_step3_title', 'Đã trả phòng'), desc: t('bd_timeline_step3_desc', 'Hoàn tất thời gian lưu trú') }
  ];

  const getStatusIndex = (statusStr) => {
    const normalized = (statusStr || '').toLowerCase().replace(/[^a-z]/g, '');
    if (normalized === 'cancelled') return -1;
    if (normalized === 'checkedout' || normalized === 'completed') return 3;
    if (normalized === 'checkedin') return 2;
    if (normalized === 'confirmed' || normalized === 'active' || normalized === 'partiallypaid' || normalized === 'paid') return 1;
    return 0; // Created/Pending
  };

  const currentStatusIdx = getStatusIndex(booking.status);

  // Formatting date/time helper
  const formatDateTime = (isoString) => {
    if (!isoString) return t('bd_dt_waiting', 'Chờ cập nhật');
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
    if (idx > currentStatusIdx) return t('bd_dt_no_event', 'Chưa diễn ra');
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
    return t('bd_dt_waiting', 'Chờ cập nhật');
  };

  const finalAmount = booking.finalAmount || booking.totalAmount || 0;
  const todayStr = new Date().toISOString().split('T')[0];
  const isFutureCheckIn = booking.checkInDate >= todayStr;
  const hasPaid = (booking.paidAmount > 0) || (booking.depositAmount > 0) || ['paid', 'partially paid', 'partially-paid'].includes(String(booking.status || '').toLowerCase());
  const canRequestRefund = hasPaid && isFutureCheckIn;
  const isQrCheckInMethod = String(booking.checkInMethod || '').toLowerCase() === 'qr code';
  const canUseQrCheckIn =
    isQrCheckInMethod &&
    booking.status !== 'Cancelled' &&
    booking.status !== 'Refund Pending' &&
    currentStatusIdx < 2;

  return (
    <>
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />
      <div className="w-full min-h-screen pt-36 pb-24 bg-gray-50 flex items-start justify-center px-4 font-['Montserrat'] dashboard-font-semibold">
        <div className="max-w-6xl w-full flex flex-col text-left">

          {/* Header Area */}
          <div className="bg-white p-6 md:p-8 border border-outline-variant shadow-md mb-8 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <button
                onClick={() => setActivePage('dashboard')}
                className="mb-4 w-fit text-xs text-secondary hover:text-primary uppercase tracking-widest font-bold flex items-center gap-1 cursor-pointer bg-transparent border-none"
              >
                <span className="material-symbols-outlined text-sm">arrow_back</span> {t('bd_back_dashboard', 'Trở lại Dashboard')}
              </button>

              <div className="flex items-center gap-3 flex-wrap">
                <h2 className="text-xl md:text-2xl font-semibold text-slate-950 uppercase tracking-wider m-0">{t('bd_title', 'Đặt phòng:')} <span className="keep-font-bold font-bold text-primary">{booking.bookingReference}</span></h2>
                <button
                  onClick={handleCopyCode}
                  className="flex items-center gap-1 bg-slate-100 hover:bg-slate-200 text-slate-700 text-[10px] font-bold px-2 py-1 uppercase tracking-wider border-none cursor-pointer transition-all"
                >
                  <span className="material-symbols-outlined text-sm">{copied ? 'check' : 'content_copy'}</span>
                  {copied ? t('bd_btn_copied', 'Đã copy') : t('bd_btn_copy', 'Copy mã')}
                </button>
              </div>
              <p className="text-xs text-slate-500 font-bold uppercase tracking-wider mt-1">{booking.roomTypeName}</p>
            </div>

            {/* Current Status Badge */}
            <div className="text-right">
              <span className="text-[10px] text-slate-400 font-black uppercase tracking-widest block mb-1">{t('db_status_label', 'Trạng thái')}</span>
              <span className={`px-4 py-2 text-xs font-black uppercase tracking-widest ${booking.status === 'Cancelled'
                ? 'bg-red-100 text-red-700'
                : booking.status === 'Checked-in' || booking.status === 'Checked In'
                  ? 'bg-blue-100 text-blue-700'
                  : booking.status === 'Checked-out' || booking.status === 'Checked Out'
                    ? 'bg-slate-200 text-slate-700'
                    : booking.status === 'Paid'
                      ? 'bg-green-100 text-green-700'
                      : booking.status === 'Refund Pending'
                        ? 'bg-amber-100 text-amber-700'
                        : booking.status === 'Partially Paid'
                          ? 'bg-indigo-100 text-indigo-700'
                          : 'bg-yellow-100 text-yellow-700'
                }`}>
                {booking.status === 'Cancelled'
                  ? t('status_cancelled', 'Đã Hủy')
                  : booking.status === 'Checked-in' || booking.status === 'Checked In'
                    ? t('status_checked_in', 'Đã nhận phòng')
                    : booking.status === 'Checked-out' || booking.status === 'Checked Out'
                      ? t('status_checked_out', 'Đã trả phòng')
                      : booking.status === 'Paid'
                        ? t('status_paid', 'Đã thanh toán')
                        : booking.status === 'Refund Pending'
                          ? 'Chờ hoàn tiền'
                          : booking.status === 'Partially Paid'
                            ? t('status_partially_paid', 'Đã cọc 30%')
                            : t('status_confirmed', 'Đã xác nhận')}
              </span>
            </div>
          </div>

          {/* Main Content Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">

            {/* Left Column: Timeline & Details (col-span-8) */}
            <div className="lg:col-span-8 bg-white border border-outline-variant shadow-md p-6 md:p-8 space-y-8">

              {/* Vertical Timeline */}
              <div>
                <h3 className="text-xs font-black text-slate-900 uppercase tracking-widest border-b border-gray-100 pb-3 mb-6">{t('bd_timeline_title', 'Tiến trình trạng thái (Timeline)')}</h3>

                {booking.status === 'Cancelled' ? (
                  <div className="bg-red-50 border border-red-200 p-4 flex items-center gap-3 text-red-700 font-bold text-xs">
                    <span className="material-symbols-outlined text-2xl">cancel</span>
                    <span>{t('bd_cancel_notice', 'Đặt phòng này đã bị hủy bỏ. Vui lòng liên hệ bộ phận hỗ trợ khách hàng Elysian nếu cần trợ giúp.')}</span>
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
                <h3 className="text-xs font-black text-slate-900 uppercase tracking-widest pb-3 mb-4">{t('bd_detail_title', 'Chi tiết phòng nghỉ & Thời gian')}</h3>
                <div className="grid grid-cols-1 sm:grid-cols-4 gap-6 text-xs font-bold text-slate-700">
                  <div>
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">{t('payment_invoice_checkin_date', 'Ngày Nhận Phòng')}</span>
                    <span className="text-slate-900 font-black tracking-wide block mt-1">{booking.checkInDate}</span>
                    <span className="text-[9px] text-slate-500 uppercase tracking-wider">{t('db_booking_checkin_time_default', 'Từ 2h trưa (14:00)')}</span>
                  </div>
                  <div>
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">{t('payment_invoice_checkout_date', 'Ngày Trả Phòng')}</span>
                    <span className="text-slate-900 font-black tracking-wide block mt-1">{booking.checkOutDate}</span>
                    <span className="text-[9px] text-slate-500 uppercase tracking-wider">{t('db_booking_checkout_time_default', 'Trước 12h trưa (12:00)')}</span>
                  </div>
                  <div>
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">{t('booking_guests_stepper_label', 'Số lượng khách')}</span>
                    <span className="text-slate-900 font-black tracking-wide block mt-1">
                      {booking.numberOfAdults || 1} {t('booking_summary_nl', 'Người lớn')}
                      {booking.numberOfChildren ? `, ${booking.numberOfChildren} ${t('booking_summary_te', 'Trẻ em')}` : ''}
                    </span>
                    <span className="text-[9px] text-slate-500 uppercase tracking-wider">
                      {booking.quantity ? `${t('booking_summary_quantity', 'Số lượng')}: ${booking.quantity} ${t('booking_summary_rooms_unit', 'phòng')}` : `${t('booking_summary_quantity', 'Số lượng')}: 1 ${t('booking_summary_rooms_unit', 'phòng')}`}
                    </span>
                  </div>
                  <div>
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block">{t('payment_invoice_checkin_method', 'Hình thức nhận phòng')}</span>
                    <span className="text-primary font-black uppercase tracking-wider block mt-1">
                      {booking.checkInMethod === 'FaceID' || booking.checkInMethod === 'Face Recognition' ? 'FaceID eKYC' : booking.checkInMethod === 'QR Code' ? t('booking_checkin_qr_option', 'Mã QR') : t('booking_checkin_manual_option', 'Quầy lễ tân')}
                    </span>
                  </div>
                </div>

                {canUseQrCheckIn && (
                  <QrCheckInCard
                    booking={booking}
                    qrTokenData={qrTokenData}
                    isGenerating={isGeneratingQr}
                    copied={copiedQr}
                    onGenerate={handleGenerateQrToken}
                    onCopy={handleCopyQrToken}
                  />
                )}

                {booking.roomAccesses?.length > 0 && (
                  <div className="mt-6">
                    <span className="block text-[9px] text-slate-400 font-bold uppercase tracking-wider mb-2">
                      Phòng và mật khẩu truy cập
                    </span>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      {booking.roomAccesses.map((access) => (
                        <div
                          key={access.roomId || access.roomNumber}
                          className="border border-slate-200 bg-slate-50 p-4 flex items-center justify-between"
                        >
                          <div>
                            <span className="block text-sm text-slate-900 font-black">
                              Phòng {access.roomNumber}
                            </span>
                            <span className="block text-[9px] text-slate-500 uppercase mt-1">
                              Tầng {access.floorNumber ?? 'N/A'}
                            </span>
                          </div>
                          <div className="text-right">
                            <span className="block text-[8px] text-slate-400 uppercase">
                              Mật khẩu
                            </span>
                            <strong className="block font-mono text-lg text-primary tracking-[0.18em]">
                              {access.roomPassword || 'Đã khóa'}
                            </strong>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {booking.specialRequests && (
                  <div className="mt-6 p-4 bg-slate-50 border border-slate-200">
                    <span className="block text-[9px] text-slate-400 font-bold uppercase tracking-wider mb-1">{t('booking_special_requests_label', 'Yêu cầu đặc biệt:')}</span>
                    <p className="text-xs text-slate-700 font-bold tracking-wide m-0">{booking.specialRequests}</p>
                  </div>
                )}
              </div>

              {/* ACTION BUTTONS BASED ON STATUS */}
              <div className="border-t border-gray-100 pt-6 flex flex-wrap gap-4">
                {/* Pay Now button (available when unpaid/confirmed/partially paid) */}
                {(booking.status === 'Confirmed' || booking.status === 'Partially Paid') && (
                  <button
                    onClick={handlePaymentRedirect}
                    className="bg-primary hover:brightness-110 text-on-primary text-xs font-black uppercase tracking-widest px-8 py-3.5 active:scale-98 transition-all cursor-pointer border-none flex items-center gap-1.5 h-11"
                  >
                    <span className="material-symbols-outlined text-lg">payment</span>
                    Thanh toán ngay
                  </button>
                )}

                {/* Cancel button (available before Checked-in) */}
                {booking.status !== 'Cancelled' && currentStatusIdx < 2 && !canRequestRefund && booking.status !== 'Refund Pending' && (
                  <button
                    onClick={handleCancelBooking}
                    className="bg-red-50 hover:bg-red-100 text-red-600 text-xs font-black uppercase tracking-widest px-8 py-3.5 active:scale-98 transition-all cursor-pointer border-none flex items-center gap-1.5 h-11"
                  >
                    <span className="material-symbols-outlined text-lg">cancel</span>
                    {t('bd_btn_cancel', 'Hủy đặt phòng')}
                  </button>
                )}

                {/* Refund button (available when Paid/Partially Paid in future) */}
                {booking.status !== 'Cancelled' && currentStatusIdx < 2 && canRequestRefund && booking.status !== 'Refund Pending' && (
                  <button
                    onClick={() => setIsRefundModalOpen(true)}
                    className="bg-rose-600 hover:bg-rose-700 text-white text-xs font-black uppercase tracking-widest px-8 py-3.5 active:scale-98 transition-all cursor-pointer border-none flex items-center gap-1.5 h-11"
                  >
                    <span className="material-symbols-outlined text-lg">payments</span>
                    Yêu cầu hoàn tiền
                  </button>
                )}

                {booking.status === 'Refund Pending' && (
                  <div className="bg-amber-50 border border-amber-200 text-amber-800 text-xs font-bold px-4 py-2.5 flex items-center gap-2">
                    <span className="material-symbols-outlined text-base">hourglass_empty</span>
                    Yêu cầu hoàn tiền đang chờ phê duyệt
                  </div>
                )}

                {/* Request button (available when Checked-in) */}
                {booking.status !== 'Cancelled' && currentStatusIdx === 2 && (
                  <>


                    {/* Room Move buttons / labels */}
                    {isRoomChangePending ? (
                      <div className="bg-blue-500/10 border border-blue-500/20 text-blue-700 backdrop-blur-md rounded-xl text-xs font-bold px-6 py-2.5 flex items-center gap-2 h-11 shadow-sm">
                        <span className="material-symbols-outlined text-base animate-pulse">hourglass_empty</span>
                        Yêu cầu đổi phòng đang chờ phê duyệt
                      </div>
                    ) : (
                      <button
                        onClick={() => {
                          setIsRoomChangeModalOpen(true);
                          setRoomChangeReason('');
                          setSelectedRoomType(null);
                          setRoomChangeOption('same_type');
                        }}
                        className="bg-blue-500/10 border border-blue-500/20 text-blue-600 hover:bg-blue-500/20 text-xs font-black uppercase tracking-widest px-6 py-3.5 active:scale-98 transition-all cursor-pointer rounded-xl flex items-center gap-1.5 h-11 shadow-sm backdrop-blur-md"
                      >
                        <span className="material-symbols-outlined text-lg">autorenew</span>
                        Yêu cầu đổi phòng
                      </button>
                    )}

                    {/* Stay Extension buttons / labels */}
                    {isStayExtensionPending ? (
                      <div className="bg-emerald-500/10 border border-emerald-500/20 text-emerald-700 backdrop-blur-md rounded-xl text-xs font-bold px-6 py-2.5 flex items-center gap-2 h-11 shadow-sm">
                        <span className="material-symbols-outlined text-base animate-pulse">hourglass_empty</span>
                        Yêu cầu gia hạn đang chờ phê duyệt
                      </div>
                    ) : (
                      <button
                        onClick={() => {
                          setIsStayExtensionModalOpen(true);
                          setNewCheckOutDate('');
                          setExtensionReason('');
                        }}
                        className="bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 hover:bg-emerald-500/20 text-xs font-black uppercase tracking-widest px-6 py-3.5 active:scale-98 transition-all cursor-pointer rounded-xl flex items-center gap-1.5 h-11 shadow-sm backdrop-blur-md"
                      >
                        <span className="material-symbols-outlined text-lg">calendar_add_on</span>
                        Yêu cầu gia hạn
                      </button>
                    )}

                    {/* Early Check-out buttons / labels */}
                    {isEarlyCheckOutPending ? (
                      <div className="bg-amber-500/10 border border-amber-500/20 text-amber-700 backdrop-blur-md rounded-xl text-xs font-bold px-6 py-2.5 flex items-center gap-2 h-11 shadow-sm">
                        <span className="material-symbols-outlined text-base animate-pulse">hourglass_empty</span>
                        Yêu cầu check-out sớm đang chờ phê duyệt
                      </div>
                    ) : (
                      <button
                        onClick={() => {
                          setIsEarlyCheckOutModalOpen(true);
                          setNewEarlyCheckOutDate('');
                          setEarlyCheckOutReason('');
                        }}
                        className="bg-amber-500/10 border border-amber-500/20 text-amber-600 hover:bg-amber-500/20 text-xs font-black uppercase tracking-widest px-6 py-3.5 active:scale-98 transition-all cursor-pointer rounded-xl flex items-center gap-1.5 h-11 shadow-sm backdrop-blur-md"
                      >
                        <span className="material-symbols-outlined text-lg">history</span>
                        Yêu cầu Checkout sớm
                      </button>
                    )}
                  </>
                )}

                {/* Feedback button (available when Checked-out / Completed) */}
                {booking.status !== 'Cancelled' && currentStatusIdx === 3 && !feedback && (
                  <button
                    onClick={handleOpenWriteFeedback}
                    className="bg-primary text-white hover:bg-opacity-95 text-xs font-black uppercase tracking-widest px-8 py-3.5 active:scale-98 transition-all cursor-pointer border-none flex items-center gap-2 h-11"
                  >
                    <MessageSquare size={16} />
                    <span>{t('bd_btn_service_review', 'Đánh giá dịch vụ')}</span>
                  </button>
                )}
              </div>

              {/* Display submitted feedback if exists */}
              {feedback && (
                <div className="border-t border-gray-100 pt-6">
                  <h3 className="text-xs font-black text-slate-900 uppercase tracking-widest pb-3 mb-4 flex items-center justify-between">
                    <span>{t('bd_feedback_title', 'Đánh giá dịch vụ của bạn')}</span>
                    <div className="flex gap-2">
                      <button
                        onClick={handleOpenEditFeedback}
                        className="flex items-center gap-1 text-[10px] font-bold uppercase tracking-wider text-slate-500 hover:text-primary border-none bg-transparent cursor-pointer"
                      >
                        <Edit3 size={12} />
                        <span>{t('bd_btn_edit', 'Sửa')}</span>
                      </button>
                      <button
                        onClick={handleDeleteFeedback}
                        className="flex items-center gap-1 text-[10px] font-bold uppercase tracking-wider text-slate-500 hover:text-red-600 border-none bg-transparent cursor-pointer"
                      >
                        <Trash2 size={12} />
                        <span>{t('bd_btn_delete', 'Xóa')}</span>
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
                        {t('bd_submitted_at', 'Đã gửi lúc')} {new Date(feedback.createdAt).toLocaleDateString('vi-VN')}
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
                            <span className="text-green-700 text-[9px] font-black uppercase tracking-wider block mb-0.5">✓ {t('review_pros', 'Ưu điểm:')}</span>
                            <span className="text-slate-600 font-medium">{feedback.pros}</span>
                          </div>
                        )}
                        {feedback.cons && (
                          <div>
                            <span className="text-red-700 text-[9px] font-black uppercase tracking-wider block mb-0.5">✗ {t('review_cons', 'Nhược điểm:')}</span>
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
                <h3 className="text-xs font-black text-slate-900 uppercase tracking-widest border-b border-gray-100 pb-3 mb-4">{t('bd_summary_title', 'Tóm tắt đặt phòng')}</h3>

                <div className="space-y-4">
                  {/* Room Info */}
                  <div>
                    <span className="text-[9px] text-primary font-black uppercase tracking-widest block mb-0.5">ELYSIAN HOTELS</span>
                    <h4 className="text-sm font-black text-slate-950 uppercase tracking-wider">{booking.roomTypeName}</h4>
                    <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider mt-0.5 block">{t('booking_ref_code_label', 'Mã')}: {booking.bookingReference}</span>
                  </div>

                  {/* Detail items */}
                  <div className="space-y-2.5 border-t border-b border-gray-100 py-4 text-xs font-bold text-slate-700">
                    <div className="flex justify-between gap-4">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('booking_summary_duration', 'Thời gian:')}</span>
                      <span className="text-right">{booking.checkInDate} {t('booking_summary_date_to', 'đến')} {booking.checkOutDate} ({booking.nights} {t('booking_summary_nights', 'đêm')})</span>
                    </div>
                    <div className="flex justify-between gap-4">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('bd_summary_rooms', 'Số phòng đặt:')}</span>
                      <span>{booking.quantity || 1} {t('booking_summary_rooms_unit', 'phòng')}</span>
                    </div>
                    <div className="flex justify-between gap-4">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('booking_guests_stepper_label', 'Số lượng khách:')}</span>
                      <span>{booking.numberOfAdults || 1} {t('booking_summary_nl', 'NL')} {booking.numberOfChildren ? `• ${booking.numberOfChildren} ${t('booking_summary_te', 'TE')}` : ''}</span>
                    </div>
                    <div className="flex justify-between gap-4">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('payment_invoice_checkin_method', 'Check-in:')}</span>
                      <span className="text-primary uppercase">{booking.checkInMethod === 'FaceID' || booking.checkInMethod === 'Face Recognition' ? 'FaceID eKYC' : booking.checkInMethod === 'QR Code' ? t('booking_checkin_qr_option', 'Mã QR') : t('booking_checkin_manual_option', 'Quầy lễ tân')}</span>
                    </div>
                  </div>

                  {/* Price breakdown */}
                  <div className="space-y-2 text-xs font-bold text-slate-700">
                    <div className="flex justify-between gap-4">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('booking_summary_subtotal', 'Tạm tính (chưa thuế):')}</span>
                      <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(booking.totalAmount || (finalAmount / 1.1))}</span>
                    </div>
                    <div className="flex justify-between gap-4">
                      <span className="text-slate-400 font-bold uppercase tracking-wider">{t('booking_summary_vat', 'Thuế VAT (10%):')}</span>
                      <span>{new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(finalAmount - (booking.totalAmount || (finalAmount / 1.1)))}</span>
                    </div>
                    <div className="flex justify-between border-t border-slate-950 pt-3 text-sm gap-4">
                      <span className="font-black text-slate-900 uppercase tracking-wider">{t('booking_summary_total', 'TỔNG CỘNG:')}</span>
                      <span className="font-black text-primary text-base">
                        {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(finalAmount)}
                      </span>
                    </div>
                    {/* Payment status badge */}
                    {booking.paidAmount != null && parseFloat(booking.paidAmount) > 0 && (() => {
                      const paid = parseFloat(booking.paidAmount || 0);
                      const total = parseFloat(finalAmount || 1);
                      const ratio = total > 0 ? paid / total : 0;
                      const isFullyPaid = ratio >= 0.999;
                      return (
                        <div className="flex justify-between items-center pt-1 gap-4">
                          <span className="text-slate-400 font-bold uppercase tracking-wider text-xs">
                            {t('bd_payment_status', 'Trạng thái thanh toán:')}
                          </span>
                          {isFullyPaid ? (
                            <span style={{
                              display: 'inline-flex', alignItems: 'center', gap: '4px',
                              background: 'linear-gradient(135deg, #16a34a, #15803d)',
                              color: '#fff', fontSize: '9px', fontWeight: 900,
                              letterSpacing: '0.08em', padding: '3px 10px',
                              borderRadius: '3px', textTransform: 'uppercase',
                              boxShadow: '0 0 8px rgba(22,163,74,0.35)'
                            }}>
                              ✓ Đã thanh toán 100%
                            </span>
                          ) : (
                            <span style={{
                              display: 'inline-flex', alignItems: 'center', gap: '4px',
                              background: 'linear-gradient(135deg, #d97706, #b45309)',
                              color: '#fff', fontSize: '9px', fontWeight: 900,
                              letterSpacing: '0.08em', padding: '3px 10px',
                              borderRadius: '3px', textTransform: 'uppercase',
                              boxShadow: '0 0 8px rgba(217,119,6,0.35)'
                            }}>
                              ⚡ Đặt cọc {Math.round(ratio * 100)}%
                            </span>
                          )}
                        </div>
                      );
                    })()}
                  </div>

                  {/* Policy instruction box */}
                  <div className="bg-slate-50 border border-slate-200 p-4 text-[9px] text-slate-500 font-semibold leading-relaxed uppercase tracking-wider mt-4">
                    <span className="block font-black text-slate-700 mb-1">{t('bd_rules_title', 'Quy định nhận/trả phòng:')}</span>
                    <p className="mb-1">{t('booking_step2_label', 'Check-in')}: {t('db_booking_checkin_time_default', 'Từ 2h trưa (14:00)')}</p>
                    <p>{t('booking_step2_label', 'Check-in')}-out: {t('db_booking_checkout_time_default', 'Trước 12h trưa (12:00)')}</p>
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
              {isEditingFeedback ? t('fm_edit_title', 'Chỉnh sửa đánh giá dịch vụ') : t('bd_feedback_title', 'Đánh giá dịch vụ của bạn')}
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

      {/* REFUND REQUEST MODAL */}
      {isRefundModalOpen && (
        <div className="fixed inset-0 z-[5000] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white border-2 border-primary/20 max-w-lg w-full p-6 relative shadow-2xl animate-scale-in text-slate-800 font-['Montserrat']">
            {/* Close Button */}
            <button
              type="button"
              onClick={() => setIsRefundModalOpen(false)}
              className="absolute top-4 right-4 text-slate-400 hover:text-slate-700 border-none bg-transparent cursor-pointer p-1"
            >
              <X size={20} />
            </button>

            <div className="bg-rose-600 text-white text-[9px] font-black uppercase tracking-widest px-3 py-1 absolute top-0 left-0">
              Yêu cầu hoàn tiền đặt phòng
            </div>

            <h3 className="text-sm font-black text-slate-900 uppercase tracking-wider mt-4 mb-1 text-left">
              {booking.roomTypeName}
            </h3>
            <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider border-b border-gray-100 pb-3 mb-4 text-left">
              Mã đặt phòng: {booking.bookingReference}
            </p>

            <div className="bg-amber-50 border border-amber-200 p-4 mb-4 text-left">
              <div className="flex gap-2 text-amber-850">
                <AlertCircle className="shrink-0 w-4 h-4 mt-0.5 text-amber-800" />
                <div className="text-xs font-semibold leading-relaxed text-amber-850">
                  <p className="font-bold mb-1 text-amber-800">Chính sách và Lưu ý hoàn tiền:</p>
                  <p className="mb-0.5 text-amber-800">1. Số tiền hoàn trả thực tế sẽ được tính toán dựa trên thời gian hủy phòng so với ngày nhận phòng thực tế.</p>
                  <p className="mb-0.5 text-amber-800">2. Đặt phòng của quý khách sẽ bị hủy ngay khi yêu cầu hoàn tiền này được phê duyệt bởi ban quản lý.</p>
                  <p className="text-amber-800">3. Quá trình xử lý giao dịch hoàn tiền có thể mất từ 1 - 3 ngày làm việc tùy thuộc vào phương thức thanh toán.</p>
                </div>
              </div>
            </div>

            <form onSubmit={handleRefundSubmit} className="space-y-4 text-left">
              <div>
                <label className="block text-[10px] font-black uppercase tracking-widest text-slate-600 mb-1.5">Lý do yêu cầu hoàn tiền:</label>
                <textarea
                  rows="4"
                  value={refundReason}
                  onChange={(e) => setRefundReason(e.target.value)}
                  placeholder="Vui lòng cung cấp chi tiết lý do bạn muốn hoàn tiền (ví dụ: Thay đổi lịch trình đột xuất, lý do sức khỏe...)"
                  className="w-full p-3 border border-slate-300 text-xs font-medium focus:border-primary focus:outline-none placeholder-slate-400 leading-relaxed resize-none rounded-none bg-slate-50"
                  required
                />
              </div>

              {/* Action Buttons */}
              <div className="flex justify-end gap-2.5 pt-2 text-[10px] font-black uppercase tracking-widest">
                <button
                  type="button"
                  onClick={() => setIsRefundModalOpen(false)}
                  className="px-5 py-2.5 border border-slate-300 text-slate-700 bg-white hover:bg-slate-100 transition-all cursor-pointer"
                >
                  Quay lại
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingRefund}
                  className="px-5 py-2.5 bg-rose-600 text-white hover:bg-rose-700 transition-all cursor-pointer border-none flex items-center gap-1.5"
                >
                  {isSubmittingRefund ? (
                    <>
                      <div className="animate-spin rounded-full h-3.5 w-3.5 border-t-2 border-white" />
                      <span>Đang gửi...</span>
                    </>
                  ) : (
                    <span>Xác nhận gửi</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* CUSTOMER ROOM CHANGE MODAL */}
      {isRoomChangeModalOpen && (
        <div className="fixed inset-0 z-[5000] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto font-['Montserrat']">
          <div className="bg-white border-2 border-primary/20 max-w-2xl w-full p-6 relative shadow-2xl animate-scale-in text-slate-800 text-left">
            {/* Close Button */}
            <button
              type="button"
              onClick={() => setIsRoomChangeModalOpen(false)}
              className="absolute top-4 right-4 text-slate-400 hover:text-slate-700 border-none bg-transparent cursor-pointer p-1"
            >
              <X size={20} />
            </button>

            <div className="bg-primary text-white text-[9px] font-black uppercase tracking-widest px-3 py-1 absolute top-0 left-0">
              Yêu cầu chuyển phòng (Room Move)
            </div>

            <h3 className="text-sm font-black text-slate-900 uppercase tracking-wider mt-4 mb-1">
              YÊU CẦU ĐỔI PHÒNG NGHỈ
            </h3>
            <p className="text-[10px] text-slate-400 font-bold uppercase tracking-wider border-b border-gray-100 pb-3 mb-4">
              Mã đặt phòng: {booking.bookingReference}
            </p>

            <form onSubmit={handleCustomerRoomChangeSubmit} className="space-y-5">
              {/* Phần 1: Phòng hiện tại & Lý do */}
              <div className="space-y-3">
                <div className="bg-slate-50 border border-slate-200 p-3.5 flex items-center gap-3">
                  <div className="w-9 h-9 bg-primary/10 flex items-center justify-center flex-shrink-0 text-primary">
                    <span className="material-symbols-outlined text-xl">bed</span>
                  </div>
                  <div>
                    <p className="text-xs font-bold text-slate-950 uppercase tracking-wide">
                      Phòng hiện tại: {booking.roomNumber || '—'}
                    </p>
                    <p className="text-[10px] text-slate-500 font-bold uppercase tracking-wider mt-0.5">
                      Loại phòng: {booking.roomTypeName} · Giá gốc: {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(booking.priceatbooking || 0)}/đêm
                    </p>
                  </div>
                </div>

                <div>
                  <label className="block text-[10px] font-black uppercase tracking-widest text-slate-600 mb-1.5">
                    Lý do đổi phòng (tùy chọn):
                  </label>
                  <textarea
                    rows="2"
                    value={roomChangeReason}
                    onChange={(e) => setRoomChangeReason(e.target.value)}
                    placeholder="Ví dụ: Phòng ồn, điều hòa kém lạnh, muốn đổi sang phòng view đẹp hơn..."
                    className="w-full p-3 border border-slate-300 text-xs font-medium focus:border-primary focus:outline-none placeholder-slate-450 leading-relaxed resize-none rounded-none bg-slate-50"
                  />
                </div>
              </div>

              {/* Phần 2: Lựa chọn hình thức đổi phòng */}
              <div className="space-y-4">
                <label className="block text-[10px] font-black uppercase tracking-widest text-slate-600">
                  Phần 2 — Lựa chọn loại phòng chuyển đổi:
                </label>
                
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <button
                    type="button"
                    onClick={() => {
                      setRoomChangeOption('same_type');
                      setSelectedRoomType(null);
                    }}
                    className={`p-4 border transition-all text-center cursor-pointer flex flex-col items-center justify-center gap-1.5 focus:outline-none ${
                      roomChangeOption === 'same_type'
                        ? 'border-primary bg-primary/5 shadow-sm'
                        : 'border-slate-200 bg-white hover:border-primary/30'
                    }`}
                  >
                    <span className="material-symbols-outlined text-lg text-primary">autorenew</span>
                    <span className="text-xs font-black uppercase tracking-wider text-slate-900">Cùng loại phòng</span>
                    <span className="text-[9px] text-green-700 font-bold uppercase tracking-wider">Miễn phí đổi phòng</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setRoomChangeOption('different_type')}
                    className={`p-4 border transition-all text-center cursor-pointer flex flex-col items-center justify-center gap-1.5 focus:outline-none ${
                      roomChangeOption === 'different_type'
                        ? 'border-primary bg-primary/5 shadow-sm'
                        : 'border-slate-200 bg-white hover:border-primary/30'
                    }`}
                  >
                    <span className="material-symbols-outlined text-lg text-primary">upgrade</span>
                    <span className="text-xs font-black uppercase tracking-wider text-slate-900">Sang hạng phòng khác</span>
                    <span className="text-[9px] text-slate-500 font-bold uppercase tracking-wider">Tính chênh lệch giá</span>
                  </button>
                </div>

                {/* Nội dung tương ứng với mỗi lựa chọn */}
                {roomChangeOption === 'same_type' ? (
                  <div className="p-4 bg-green-50 border border-green-200 text-green-800 text-xs font-semibold leading-relaxed">
                    💡 Hệ thống sẽ tự động tìm và sắp xếp một phòng trống khác có **cùng hạng phòng** ({booking.roomTypeName}) cho bạn. Yêu cầu chuyển này hoàn toàn miễn phí và không làm phát sinh thêm bất kỳ phụ phí nào.
                  </div>
                ) : (
                  <div className="space-y-3">
                    <span className="block text-[9px] text-slate-400 font-black uppercase tracking-widest">
                      Chọn hạng phòng mong muốn:
                    </span>
                    {isLoadingRooms ? (
                      <div className="space-y-2 py-4">
                        {[1, 2].map((i) => (
                          <div key={i} className="border border-slate-200 p-4 animate-pulse bg-slate-50">
                            <div className="h-4 bg-slate-200 w-1/3 mb-2" />
                            <div className="h-3 bg-slate-200 w-1/2" />
                          </div>
                        ))}
                      </div>
                    ) : availableRoomTypes.filter(rt => rt.id !== booking.roomTypeId).length === 0 ? (
                      <div className="text-center py-6 bg-slate-50 border border-slate-200 text-slate-400">
                        <span className="material-symbols-outlined text-3xl block mb-1">sentiment_dissatisfied</span>
                        <p className="text-xs font-bold uppercase tracking-wider">Hiện không có hạng phòng khác khả dụng</p>
                      </div>
                    ) : (
                      <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                        {availableRoomTypes
                          .filter(rt => rt.id !== booking.roomTypeId)
                          .map((roomType) => {
                            const isSelected = selectedRoomType?.id === roomType.id;
                            const currentRoomPrice = booking.priceatbooking || (booking.totalAmount / (booking.nights || 1) / (booking.quantity || 1));
                            const priceDiff = (roomType.baseprice || 0) - currentRoomPrice;

                            return (
                              <button
                                key={roomType.id}
                                type="button"
                                onClick={() => setSelectedRoomType(roomType)}
                                className={`w-full text-left p-3.5 border transition-all flex items-center justify-between gap-4 rounded-none cursor-pointer focus:outline-none ${
                                  isSelected
                                    ? 'border-primary bg-primary/5 shadow-sm'
                                    : 'border-slate-200 bg-white hover:border-primary/50'
                                }`}
                              >
                                <div className="space-y-0.5">
                                  <div className="text-xs font-black uppercase text-slate-900">{roomType.name}</div>
                                  <div className="text-[10px] text-slate-500 font-bold">
                                    Đơn giá: {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(roomType.baseprice || 0)}/đêm
                                  </div>
                                </div>
                                <div className="text-right">
                                  <span className={`text-[10px] font-black uppercase tracking-wider ${priceDiff >= 0 ? 'text-rose-600' : 'text-green-700'}`}>
                                    {priceDiff === 0 ? 'Bằng giá' : priceDiff > 0 ? `+${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(priceDiff)}/đêm` : `Giảm ${new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Math.abs(priceDiff))}/đêm`}
                                  </span>
                                </div>
                              </button>
                            );
                          })}
                      </div>
                    )}
                  </div>
                )}
              </div>

              {roomChangeError && (
                <div className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs font-bold leading-relaxed">
                  ⚠️ {roomChangeError}
                </div>
              )}

              {/* Action Buttons */}
              <div className="flex justify-end gap-2.5 pt-3 border-t border-slate-100 text-[10px] font-black uppercase tracking-widest">
                <button
                  type="button"
                  onClick={() => setIsRoomChangeModalOpen(false)}
                  className="px-5 py-2.5 border border-slate-300 text-slate-700 bg-white hover:bg-slate-100 transition-all cursor-pointer"
                >
                  Quay lại
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingRoomChange}
                  className="px-5 py-2.5 bg-primary text-white hover:bg-opacity-95 transition-all cursor-pointer border-none flex items-center gap-1.5"
                >
                  {isSubmittingRoomChange ? (
                    <>
                      <div className="animate-spin rounded-full h-3.5 w-3.5 border-t-2 border-white" />
                      <span>Đang gửi...</span>
                    </>
                  ) : (
                    <span>Xác nhận gửi yêu cầu</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* STAY EXTENSION MODAL */}
      {isStayExtensionModalOpen && (
        <div className="fixed inset-0 bg-slate-900 bg-opacity-70 z-50 flex items-center justify-center p-4 backdrop-blur-sm animate-fade-in font-['Montserrat']">
          <div className="bg-white border border-slate-200 max-w-md w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl animate-scale-in text-slate-900 text-left">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase">GIA HẠN LƯU TRÚ</span>
                <h4 className="text-sm font-black uppercase text-slate-900 m-0 mt-0.5">{booking.bookingReference}</h4>
              </div>
              <button
                onClick={() => setIsStayExtensionModalOpen(false)}
                className="text-slate-400 hover:text-slate-900 border-none bg-transparent cursor-pointer flex items-center"
              >
                <X size={18} />
              </button>
            </div>

            <div className="bg-amber-50 border border-amber-200 p-4 text-xs font-semibold leading-relaxed text-amber-800">
              💡 <strong>Thông tin hiện tại:</strong> Ngày trả phòng dự kiến là <strong>{booking.checkOutDate}</strong>. Yêu cầu gia hạn sẽ được quầy lễ tân kiểm tra và phản hồi sớm nhất.
            </div>

            <form onSubmit={handleStayExtensionSubmit} className="space-y-4">
              <div>
                <label className="block text-[10px] font-black uppercase tracking-widest text-slate-500 mb-1">Ngày trả phòng mới mong muốn:</label>
                <input
                  type="date"
                  value={newCheckOutDate}
                  min={booking.checkOutDate}
                  onChange={(e) => setNewCheckOutDate(e.target.value)}
                  className="w-full p-2.5 border border-slate-300 text-xs font-medium focus:border-primary focus:outline-none rounded-none bg-slate-50"
                  required
                />
              </div>

              <div>
                <label className="block text-[10px] font-black uppercase tracking-widest text-slate-500 mb-1">Lý do gia hạn (tùy chọn):</label>
                <textarea
                  rows="3"
                  value={extensionReason}
                  onChange={(e) => setExtensionReason(e.target.value)}
                  placeholder="Ví dụ: Thay đổi lịch trình bay, muốn ở lại trải nghiệm thêm..."
                  className="w-full p-2.5 border border-slate-300 text-xs font-medium focus:border-primary focus:outline-none placeholder-slate-400 leading-relaxed resize-none rounded-none bg-slate-50"
                />
              </div>

              <div className="flex justify-end gap-3 pt-3 border-t border-slate-100 text-[10px] font-black uppercase tracking-widest">
                <button
                  type="button"
                  onClick={() => setIsStayExtensionModalOpen(false)}
                  className="px-5 py-2.5 border border-slate-300 text-slate-700 bg-white hover:bg-slate-100 transition-all cursor-pointer"
                >
                  Quay lại
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingExtension}
                  className="px-5 py-2.5 bg-primary text-white hover:bg-opacity-95 transition-all cursor-pointer border-none flex items-center gap-1.5"
                >
                  {isSubmittingExtension ? 'Đang gửi...' : 'Gửi yêu cầu'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* EARLY CHECKOUT MODAL */}
      {isEarlyCheckOutModalOpen && (
        <div className="fixed inset-0 bg-slate-900 bg-opacity-70 z-50 flex items-center justify-center p-4 backdrop-blur-sm animate-fade-in font-['Montserrat']">
          <div className="bg-white border border-slate-200 max-w-md w-full p-6 md:p-8 flex flex-col gap-5 shadow-2xl animate-scale-in text-slate-900 text-left">
            <div className="flex justify-between items-center border-b border-slate-100 pb-3">
              <div>
                <span className="text-[9px] font-black tracking-widest text-primary uppercase">CHECK-OUT SỚM</span>
                <h4 className="text-sm font-black uppercase text-slate-900 m-0 mt-0.5">{booking.bookingReference}</h4>
              </div>
              <button
                onClick={() => setIsEarlyCheckOutModalOpen(false)}
                className="text-slate-400 hover:text-slate-900 border-none bg-transparent cursor-pointer flex items-center"
              >
                <X size={18} />
              </button>
            </div>

            <div className="bg-amber-50 border border-amber-200 p-4 text-xs font-semibold leading-relaxed text-amber-800">
              💡 <strong>Thông tin hiện tại:</strong> Ngày trả phòng dự kiến ban đầu là <strong>{booking.checkOutDate}</strong>. Yêu cầu rút ngắn ngày trả phòng sẽ được Lễ tân phê duyệt và tính lại hóa đơn tiền phòng.
            </div>

            <form onSubmit={handleEarlyCheckOutSubmit} className="space-y-4">
              <div>
                <label className="block text-[10px] font-black uppercase tracking-widest text-slate-500 mb-1">Ngày trả phòng mới mong muốn:</label>
                <input
                  type="date"
                  value={newEarlyCheckOutDate}
                  max={booking.checkOutDate}
                  min={booking.checkInDate}
                  onChange={(e) => setNewEarlyCheckOutDate(e.target.value)}
                  className="w-full p-2.5 border border-slate-300 text-xs font-medium focus:border-primary focus:outline-none rounded-none bg-slate-50"
                  required
                />
              </div>

              <div>
                <label className="block text-[10px] font-black uppercase tracking-widest text-slate-500 mb-1">Lý do check-out sớm (tùy chọn):</label>
                <textarea
                  rows="3"
                  value={earlyCheckOutReason}
                  onChange={(e) => setEarlyCheckOutReason(e.target.value)}
                  placeholder="Ví dụ: Thay đổi lịch trình gấp, việc gia đình đột xuất..."
                  className="w-full p-2.5 border border-slate-300 text-xs font-medium focus:border-primary focus:outline-none placeholder-slate-400 leading-relaxed resize-none rounded-none bg-slate-50"
                />
              </div>

              <div className="flex justify-end gap-3 pt-3 border-t border-slate-100 text-[10px] font-black uppercase tracking-widest">
                <button
                  type="button"
                  onClick={() => setIsEarlyCheckOutModalOpen(false)}
                  className="px-5 py-2.5 border border-slate-300 text-slate-700 bg-white hover:bg-slate-100 transition-all cursor-pointer"
                >
                  Quay lại
                </button>
                <button
                  type="submit"
                  disabled={isSubmittingEarlyCheckOut}
                  className="px-5 py-2.5 bg-primary text-white hover:bg-opacity-95 transition-all cursor-pointer border-none flex items-center gap-1.5"
                >
                  {isSubmittingEarlyCheckOut ? 'Đang gửi...' : 'Gửi yêu cầu'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* CUSTOM CANCEL CONFIRMATION DIALOG */}
      {isCancelConfirmOpen && (
        <div className="fixed inset-0 bg-slate-900 bg-opacity-70 z-[6000] flex items-center justify-center p-4 backdrop-blur-sm animate-fade-in font-['Montserrat']">
          <div className="bg-white border border-slate-200 max-w-sm w-full p-6 flex flex-col gap-4 shadow-2xl animate-scale-in text-slate-950 text-left">
            <div className="flex items-center gap-2 text-rose-600">
              <span className="material-symbols-outlined text-lg">warning</span>
              <span className="text-[10px] font-black uppercase tracking-widest">{t('bd_confirm_cancel_title', 'XÁC NHẬN HỦY ĐẶT PHÒNG')}</span>
            </div>
            <p className="text-xs text-slate-600 font-semibold leading-relaxed m-0">
              {t('bd_confirm_cancel_message', 'Bạn có chắc chắn muốn hủy đơn đặt phòng này không? Hành động này sẽ thay đổi trạng thái đơn của bạn và không thể hoàn tác.')}
            </p>
            <div className="p-3 bg-amber-50 border border-amber-200/80 rounded-xl text-amber-800 text-[11px] font-semibold leading-relaxed flex items-start gap-2">
              <span className="material-symbols-outlined text-base text-amber-600 shrink-0 mt-0.5">info</span>
              <div>
                <strong>Lưu ý chính sách hủy phòng:</strong> Tỷ lệ hoàn tiền sẽ được tự động tính toán dựa trên số ngày còn lại đến ngày Check-in. Nếu hủy sát ngày hoặc trong ngày nhận phòng, số tiền được hoàn lại có thể là <strong>0%</strong>.
              </div>
            </div>
            <div className="flex justify-end gap-2.5 pt-2 text-[10px] font-black uppercase tracking-widest">
              <button
                type="button"
                onClick={() => setIsCancelConfirmOpen(false)}
                className="px-4 py-2.5 border border-slate-300 text-slate-700 bg-white hover:bg-slate-100 transition-all cursor-pointer"
              >
                {t('bd_btn_back', 'Quay lại')}
              </button>
              <button
                type="button"
                onClick={confirmCancelBooking}
                disabled={isSubmittingCancel}
                className="px-4 py-2.5 bg-rose-600 hover:bg-rose-700 text-white transition-all cursor-pointer border-none flex items-center gap-1.5"
              >
                {isSubmittingCancel ? (
                  <>
                    <div className="animate-spin rounded-full h-3 w-3 border-t-2 border-white" />
                    <span>{t('bd_btn_cancelling', 'Đang hủy...')}</span>
                  </>
                ) : (
                  <span>{t('bd_btn_confirm_cancel', 'Đồng ý hủy')}</span>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* IN-STAY SERVICE REQUEST MODAL */}
      {isServiceModalOpen && (
        <div className="fixed inset-0 bg-slate-900 bg-opacity-70 z-[5000] flex items-center justify-center p-4 backdrop-blur-sm animate-fade-in font-['Montserrat']">
          <div className="bg-white border border-slate-200 max-w-lg w-full p-6 flex flex-col gap-6 shadow-2xl animate-scale-in text-slate-950 text-left max-h-[90vh] overflow-y-auto">
            <div className="flex justify-between items-center border-b border-slate-100 pb-4">
              <div className="flex items-center gap-2 text-purple-700">
                <span className="material-symbols-outlined text-2xl">room_service</span>
                <h3 className="text-sm font-black uppercase tracking-widest m-0">GỌI DỊCH VỤ PHÒNG & TIỆN ÍCH</h3>
              </div>
              <button
                type="button"
                onClick={() => setIsServiceModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 border-none bg-transparent cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Form gửi yêu cầu dịch vụ mới */}
            <form onSubmit={handleSubmitServiceRequest} className="space-y-4">
              <div>
                <label className="block text-[10px] font-black uppercase tracking-widest text-slate-600 mb-1">
                  Chọn dịch vụ:
                </label>
                <select
                  value={selectedServiceId}
                  onChange={(e) => setSelectedServiceId(e.target.value)}
                  className="w-full p-3 border border-slate-300 text-xs font-semibold focus:border-purple-600 focus:outline-none bg-slate-50 rounded-xl"
                  required
                >
                  <option value="">-- Chọn dịch vụ --</option>
                  {availableServices.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name} - {Number(s.price).toLocaleString()} VNĐ / {s.unit}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-[10px] font-black uppercase tracking-widest text-slate-600 mb-1">
                  Số lượng:
                </label>
                <input
                  type="number"
                  min="1"
                  value={serviceQuantity}
                  onChange={(e) => setServiceQuantity(Math.max(1, parseInt(e.target.value) || 1))}
                  className="w-full p-3 border border-slate-300 text-xs font-bold focus:border-purple-600 focus:outline-none bg-slate-50 rounded-xl"
                  required
                />
              </div>

              <div>
                <label className="block text-[10px] font-black uppercase tracking-widest text-slate-600 mb-1">
                  Ghi chú (Tùy chọn):
                </label>
                <textarea
                  rows="2"
                  value={serviceNote}
                  onChange={(e) => setServiceNote(e.target.value)}
                  placeholder="Ví dụ: Mang lên phòng 302 lúc 20h00, không cay..."
                  className="w-full p-3 border border-slate-300 text-xs font-medium focus:border-purple-600 focus:outline-none bg-slate-50 rounded-xl resize-none"
                />
              </div>

              <div className="p-3 bg-purple-50 border border-purple-200 text-purple-800 text-[11px] font-semibold rounded-xl flex items-center gap-2">
                <span className="material-symbols-outlined text-base">info</span>
                <span>Dịch vụ được phê duyệt sẽ được tự động cộng vào hóa đơn thanh toán khi Checkout.</span>
              </div>

              <button
                type="submit"
                disabled={isSubmittingServiceRequest}
                className="w-full py-3 bg-purple-600 hover:bg-purple-700 text-white font-black text-xs uppercase tracking-widest rounded-xl transition-all cursor-pointer border-none shadow-md flex items-center justify-center gap-2"
              >
                {isSubmittingServiceRequest ? 'Đang gửi yêu cầu...' : 'Gửi Yêu Cầu Dịch Vụ'}
              </button>
            </form>

            {/* Lịch sử yêu cầu dịch vụ của phòng này */}
            <div className="border-t border-slate-200 pt-4 mt-2">
              <h4 className="text-xs font-black text-slate-800 uppercase tracking-wider mb-3">
                Lịch sử yêu cầu dịch vụ phòng này ({myServiceRequests.length})
              </h4>

              {isLoadingServices ? (
                <div className="text-center py-4 text-xs text-slate-500">Đang tải lịch sử...</div>
              ) : myServiceRequests.length === 0 ? (
                <div className="text-center py-4 text-xs text-slate-400 bg-slate-50 rounded-xl">
                  Chưa có yêu cầu dịch vụ nào cho phòng này.
                </div>
              ) : (
                <div className="space-y-3 max-h-60 overflow-y-auto pr-1">
                  {myServiceRequests.map((req) => (
                    <div
                      key={req.requestId}
                      className="p-3 border border-slate-200 rounded-xl bg-slate-50 flex flex-col gap-1 text-xs"
                    >
                      <div className="flex justify-between items-center">
                        <span className="font-bold text-slate-900">{req.description}</span>
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-black uppercase ${
                            req.status === 'Approved'
                              ? 'bg-emerald-100 text-emerald-700'
                              : req.status === 'Rejected'
                              ? 'bg-rose-100 text-rose-700'
                              : 'bg-amber-100 text-amber-700'
                          }`}
                        >
                          {req.status === 'Approved'
                            ? 'Đã duyệt & Cộng hóa đơn'
                            : req.status === 'Rejected'
                            ? 'Từ chối'
                            : 'Đang chờ lễ tân'}
                        </span>
                      </div>
                      {req.rejectionReason && (
                        <div className="text-rose-600 text-[11px]">Lý do từ chối: {req.rejectionReason}</div>
                      )}
                      <div className="text-[10px] text-slate-400 mt-1">
                        Tạo lúc: {new Date(req.createdAt).toLocaleString('vi-VN')}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
}
