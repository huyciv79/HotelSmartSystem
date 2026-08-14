import { useState, useEffect, useRef, useMemo } from 'react';
import { Star, AlertCircle, Filter, ChevronDown, Image, MessageSquare, Edit3, Trash2, Camera, X, PlusCircle } from 'lucide-react';
import { getReviews, createReview, updateReview, deleteReview } from '../../services/reviewService';
import { getBookingHistory } from '../../services/bookingService';
import { useLanguage } from '../../context/LanguageContext';

export default function ReviewSection({ roomId }) {
  const { t, language } = useLanguage();
  const [reviews, setReviews] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  // User and Auth State
  const token = localStorage.getItem('accessToken');
  const isLoggedIn = !!token;
  const userStr = localStorage.getItem('user');
  const currentUser = useMemo(() => {
    try {
      return userStr ? JSON.parse(userStr) : null;
    } catch (e) {
      return null;
    }
  }, [userStr]);

  // Eligible Bookings & User Bookings State
  const [eligibleBookings, setEligibleBookings] = useState([]);
  const [userBookingIds, setUserBookingIds] = useState([]);

  // Filter states
  const [filterRating, setFilterRating] = useState('all'); // 'all', 5, 4, 3, 2, 1
  const [filterHasImages, setFilterHasImages] = useState(false);
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const filterRef = useRef(null);

  // Modal Feedback States (Create / Edit)
  const [isFeedbackOpen, setIsFeedbackOpen] = useState(false);
  const [isEditingFeedback, setIsEditingFeedback] = useState(false);
  const [editingFeedbackId, setEditingFeedbackId] = useState(null);
  const [selectedBookingId, setSelectedBookingId] = useState(null);
  const [feedbackRating, setFeedbackRating] = useState(5);
  const [feedbackHoveredRating, setFeedbackHoveredRating] = useState(0);
  const [feedbackComment, setFeedbackComment] = useState('');
  const [feedbackPros, setFeedbackPros] = useState('');
  const [feedbackCons, setFeedbackCons] = useState('');
  const [feedbackImages, setFeedbackImages] = useState([]);
  const [isSubmittingFeedback, setIsSubmittingFeedback] = useState(false);
  const [feedbackErrorMessage, setFeedbackErrorMessage] = useState('');

  // Delete Modal States
  const [deletingFeedbackId, setDeletingFeedbackId] = useState(null);
  const [isDeletingFeedback, setIsDeletingFeedback] = useState(false);

  // Lightbox state
  const [lightbox, setLightbox] = useState({ isOpen: false, images: [], index: 0 });

  useEffect(() => {
    function handleClickOutside(event) {
      if (filterRef.current && !filterRef.current.contains(event.target)) {
        setIsFilterOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Fetch reviews when roomId changes
  const fetchReviewsData = () => {
    if (roomId) {
      setIsLoading(true);
      getReviews(roomId)
        .then((loaded) => {
          setReviews(loaded);
        })
        .catch((err) => {
          console.error('Lỗi khi tải đánh giá từ API:', err);
        })
        .finally(() => {
          setIsLoading(false);
        });
    }
  };

  useEffect(() => {
    fetchReviewsData();
  }, [roomId]);

  // Fetch user booking history to find eligible unreviewed COMPLETED stays for this room type
  useEffect(() => {
    if (!isLoggedIn || !roomId) {
      setEligibleBookings([]);
      setUserBookingIds([]);
      return;
    }

    getBookingHistory()
      .then((res) => {
        if (res && res.data) {
          const userBks = res.data || [];
          const allIds = userBks.map((b) => b.bookingId);
          setUserBookingIds(allIds);

          // Find completed bookings for this roomId that don't have a review yet
          const matched = userBks.filter((b) => {
            const localStatus = localStorage.getItem(`booking_status_${b.bookingId}`) || b.status;
            const isCompleted =
              localStatus === 'COMPLETED' ||
              localStatus === 'Completed' ||
              localStatus === 'Checked-out';
            const isSameRoom = Number(b.roomTypeId) === Number(roomId);
            const hasNoFeedback = !reviews.some(
              (r) => Number(r.bookingId) === Number(b.bookingId)
            );
            return isSameRoom && isCompleted && hasNoFeedback;
          });

          setEligibleBookings(matched);
        }
      })
      .catch((err) => {
        console.error('Lỗi khi kiểm tra lịch sử đặt phòng:', err);
      });
  }, [roomId, reviews, isLoggedIn]);

  // Statistics calculation
  const totalReviews = reviews.length;
  const averageRating =
    totalReviews > 0
      ? (reviews.reduce((acc, curr) => acc + curr.rating, 0) / totalReviews).toFixed(1)
      : '0.0';

  const starDistribution = [5, 4, 3, 2, 1].map((stars) => {
    const count = reviews.filter((r) => r.rating === stars).length;
    const percentage = totalReviews > 0 ? (count / totalReviews) * 100 : 0;
    return { stars, count, percentage };
  });

  // Filtered reviews calculation
  const filteredReviews = useMemo(() => {
    return reviews.filter((review) => {
      if (filterRating !== 'all' && review.rating !== Number(filterRating)) {
        return false;
      }
      if (filterHasImages && (!review.images || review.images.length === 0)) {
        return false;
      }
      return true;
    });
  }, [reviews, filterRating, filterHasImages]);

  // Lightbox control
  const openLightbox = (imagesList, index) => {
    setLightbox({ isOpen: true, images: imagesList, index });
  };

  const closeLightbox = () => {
    setLightbox({ isOpen: false, images: [], index: 0 });
  };

  const nextLightboxImage = (e) => {
    e.stopPropagation();
    setLightbox((prev) => ({
      ...prev,
      index: (prev.index + 1) % prev.images.length,
    }));
  };

  const prevLightboxImage = (e) => {
    e.stopPropagation();
    setLightbox((prev) => ({
      ...prev,
      index: (prev.index - 1 + prev.images.length) % prev.images.length,
    }));
  };

  // Open modal to Create Feedback
  const handleOpenCreateFeedback = () => {
    if (!eligibleBookings.length) return;
    setIsEditingFeedback(false);
    setEditingFeedbackId(null);
    setSelectedBookingId(eligibleBookings[0].bookingId);
    setFeedbackRating(5);
    setFeedbackHoveredRating(0);
    setFeedbackComment('');
    setFeedbackPros('');
    setFeedbackCons('');
    setFeedbackImages([]);
    setFeedbackErrorMessage('');
    setIsFeedbackOpen(true);
  };

  // Helper to compress image files before sending to server
  const compressImageFile = (file, maxWidth = 1000, maxHeight = 1000, quality = 0.75) => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (event) => {
        const img = new window.Image();
        img.onload = () => {
          let width = img.width;
          let height = img.height;

          if (width > maxWidth || height > maxHeight) {
            if (width / height > maxWidth / maxHeight) {
              height = Math.round((height * maxWidth) / width);
              width = maxWidth;
            } else {
              width = Math.round((width * maxHeight) / height);
              height = maxHeight;
            }
          }

          const canvas = document.createElement('canvas');
          canvas.width = width;
          canvas.height = height;
          const ctx = canvas.getContext('2d');
          ctx.drawImage(img, 0, 0, width, height);

          const dataUrl = canvas.toDataURL('image/jpeg', quality);
          resolve(dataUrl);
        };
        img.onerror = (err) => reject(err);
        img.src = event.target.result;
      };
      reader.onerror = (err) => reject(err);
      reader.readAsDataURL(file);
    });
  };

  // Open modal to Edit Feedback
  const handleOpenEditFeedback = (review) => {
    setIsEditingFeedback(true);
    setEditingFeedbackId(review.id);
    const bkId = review.bookingId || (eligibleBookings.length > 0 ? eligibleBookings[0].bookingId : null);
    setSelectedBookingId(bkId);
    setFeedbackRating(review.rating || 5);
    setFeedbackHoveredRating(0);
    setFeedbackComment(review.comment || '');
    setFeedbackPros(review.pros || '');
    setFeedbackCons(review.cons || '');
    setFeedbackImages(review.images || []);
    setFeedbackErrorMessage('');
    setIsFeedbackOpen(true);
  };

  // Image Upload Handling with client-side canvas compression
  const handleImageChange = (e) => {
    const files = Array.from(e.target.files);
    if (!files.length) return;

    if (feedbackImages.length + files.length > 5) {
      setFeedbackErrorMessage('Bạn chỉ có thể đính kèm tối đa 5 hình ảnh.');
      return;
    }

    const MAX_FILE_SIZE = 15 * 1024 * 1024; // 15MB
    const oversized = files.find((f) => f.size > MAX_FILE_SIZE);
    if (oversized) {
      setFeedbackErrorMessage(`Tệp "${oversized.name}" vượt quá dung lượng tối đa 15MB. Vui lòng chọn ảnh khác.`);
      return;
    }

    const promises = files.map((file) => compressImageFile(file));

    Promise.all(promises)
      .then((base64s) => {
        setFeedbackImages((prev) => [...prev, ...base64s]);
        setFeedbackErrorMessage('');
      })
      .catch((err) => {
        console.error('Lỗi khi nén file ảnh:', err);
        setFeedbackErrorMessage('Không thể xử lý hình ảnh này. Vui lòng chọn ảnh khác.');
      });
  };

  const removeImage = (index) => {
    setFeedbackImages((prev) => prev.filter((_, i) => i !== index));
  };

  // Submit Feedback (Create or Update)
  const handleSubmitFeedback = async (e) => {
    e.preventDefault();
    if (!feedbackComment.trim()) {
      setFeedbackErrorMessage('Vui lòng điền nội dung nhận xét của bạn.');
      return;
    }
    if (feedbackComment.length > 1000) {
      setFeedbackErrorMessage('Review comment cannot exceed 1000 characters.');
      return;
    }
    if (!selectedBookingId) {
      setFeedbackErrorMessage('Vui lòng chọn đơn đặt phòng tương ứng.');
      return;
    }

    setIsSubmittingFeedback(true);
    setFeedbackErrorMessage('');

    try {
      const data = {
        bookingId: selectedBookingId,
        rating: feedbackRating,
        comment: feedbackComment.trim(),
        pros: feedbackPros.trim(),
        cons: feedbackCons.trim(),
        images: feedbackImages,
      };

      if (isEditingFeedback && editingFeedbackId) {
        await updateReview(editingFeedbackId, data);
      } else {
        await createReview(selectedBookingId, data);
      }

      setIsFeedbackOpen(false);
      fetchReviewsData();
    } catch (err) {
      console.error('Error submitting feedback:', err);
      const backendMsg = err.response?.data?.message || err.message || 'Có lỗi xảy ra khi gửi đánh giá. Vui lòng thử lại.';
      setFeedbackErrorMessage(backendMsg);
    } finally {
      setIsSubmittingFeedback(false);
    }
  };

  // Delete Feedback
  const handleDeleteFeedback = async () => {
    if (!deletingFeedbackId) return;
    setIsDeletingFeedback(true);
    try {
      await deleteReview(deletingFeedbackId);
      setDeletingFeedbackId(null);
      fetchReviewsData();
    } catch (err) {
      console.error('Error deleting feedback:', err);
      alert('Không thể xóa đánh giá lúc này. Vui lòng thử lại sau.');
    } finally {
      setIsDeletingFeedback(false);
    }
  };

  // Helper check if review belongs to current user
  const isMyReview = (review) => {
    if (!isLoggedIn) return false;
    if (review.customerEmail && currentUser?.email) {
      if (review.customerEmail.toLowerCase() === currentUser.email.toLowerCase()) {
        return true;
      }
    }
    if (review.bookingId && userBookingIds.includes(Number(review.bookingId))) {
      return true;
    }
    return false;
  };

  return (
    <div className="mt-12 border-t border-slate-200 pt-10 font-['Montserrat'] text-slate-800">
      {/* Section Title & Action Button */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <h3 className="text-xl font-semibold uppercase tracking-wider text-slate-900 flex items-center gap-2">
          <span>{t('review_title', 'Xem Đánh Giá')}</span>
          <span className="text-sm font-semibold bg-primary/10 text-primary px-2.5 py-0.5 rounded-full">
            {totalReviews}
          </span>
        </h3>

        {/* Create Review Button when user has eligible COMPLETED bookings */}
        {isLoggedIn && eligibleBookings.length > 0 && (
          <button
            onClick={handleOpenCreateFeedback}
            className="bg-primary text-white hover:bg-opacity-95 text-xs font-black uppercase tracking-widest px-5 py-2.5 transition-all cursor-pointer border-none flex items-center justify-center gap-2 shadow-md active:scale-98"
          >
            <PlusCircle size={16} />
            <span>Viết đánh giá ({eligibleBookings.length})</span>
          </button>
        )}
      </div>

      {/* Review Stats Header */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-8 mb-10 bg-slate-50 p-6 border border-slate-200/60">
        <div className="md:col-span-4 flex flex-col items-center justify-center text-center border-b md:border-b-0 md:border-r border-slate-200 pb-6 md:pb-0">
          <span className="text-5xl font-semibold text-slate-900 leading-none">{averageRating}</span>
          <div className="flex gap-1 my-3">
            {[1, 2, 3, 4, 5].map((star) => (
              <Star
                key={star}
                size={18}
                className={
                  star <= Math.round(Number(averageRating))
                    ? 'fill-amber-400 text-amber-400'
                    : 'text-slate-300'
                }
              />
            ))}
          </div>
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
            {t('review_average_rating', 'Đánh giá trung bình')}
          </span>
        </div>

        <div className="md:col-span-8 flex flex-col justify-center space-y-2">
          {starDistribution.map(({ stars, count, percentage }) => (
            <div key={stars} className="flex items-center text-xs font-semibold text-slate-600 gap-3">
              <span className="w-12 text-right flex items-center gap-1 justify-end">
                {stars} <Star size={12} className="fill-amber-400 text-amber-400 inline" />
              </span>
              <div className="flex-grow h-2 bg-slate-200 overflow-hidden">
                <div
                  className="h-full bg-amber-400 transition-all duration-500"
                  style={{ width: `${percentage}%` }}
                />
              </div>
              <span className="w-10 text-slate-400">{count}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Dynamic Status / Banner for writing review */}
      <div className="mb-10 bg-white border border-primary/20 p-5 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 shadow-sm">
        <div className="flex items-start gap-4">
          <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary shrink-0">
            <MessageSquare size={18} />
          </div>
          <div>
            <h4 className="text-xs font-semibold text-slate-900 uppercase tracking-wider mb-1">
              {t('review_how_to_write_title', 'Muốn chia sẻ trải nghiệm của bạn?')}
            </h4>
            <p className="text-[10px] text-slate-500 font-semibold uppercase tracking-wide leading-relaxed">
              {isLoggedIn
                ? eligibleBookings.length > 0
                  ? `Bạn có ${eligibleBookings.length} chuyến đi đã hoàn tất tại loại phòng này chưa gửi đánh giá. Nhấn nút dưới đây để tạo đánh giá ngay!`
                  : 'Chỉ những khách hàng đã hoàn tất thủ tục trả phòng (Checked Out) mới có thể gửi đánh giá dịch vụ.'
                : 'Đăng nhập và hoàn tất chuyến đi (Checked Out) tại phòng này để gửi đánh giá trải nghiệm.'}
            </p>
          </div>
        </div>

        {isLoggedIn && eligibleBookings.length > 0 && (
          <button
            onClick={handleOpenCreateFeedback}
            className="bg-primary text-white hover:bg-opacity-95 text-[10px] font-black uppercase tracking-widest px-4 py-2 transition-all cursor-pointer border-none shrink-0"
          >
            Đánh giá ngay
          </button>
        )}
      </div>

      {/* Filter Bar */}
      <div className="mb-6 flex justify-between items-center relative" ref={filterRef}>
        <div className="relative">
          <button
            type="button"
            onClick={() => setIsFilterOpen((prev) => !prev)}
            className={`px-4 py-2 text-xs font-semibold uppercase tracking-widest border transition-all duration-200 cursor-pointer flex items-center gap-2 ${
              isFilterOpen || filterRating !== 'all' || filterHasImages
                ? 'bg-primary text-white border-primary shadow-sm'
                : 'bg-white text-slate-700 border-slate-300 hover:bg-slate-50'
            }`}
          >
            <Filter size={14} />
            <span>{t('review_filter_btn', 'Bộ lọc')}</span>
            {(filterRating !== 'all' || filterHasImages) && (
              <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse" />
            )}
            <ChevronDown
              size={14}
              className={`transition-transform duration-200 ${isFilterOpen ? 'rotate-180' : ''}`}
            />
          </button>

          {/* Dropdown Menu */}
          {isFilterOpen && (
            <div className="absolute left-0 mt-2 w-72 bg-white border border-slate-200 shadow-xl p-4 z-30 animate-scale-in font-['Montserrat']">
              {/* Star Rating Section */}
              <div className="mb-4">
                <span className="block text-[10px] font-semibold uppercase tracking-widest text-slate-400 mb-2">
                  {t('review_filter_stars', 'Số sao')}
                </span>
                <div className="grid grid-cols-2 gap-1.5">
                  {[
                    { id: 'all', label: t('review_filter_all', 'Tất cả') },
                    { id: 5, label: '5 Sao' },
                    { id: 4, label: '4 Sao' },
                    { id: 3, label: '3 Sao' },
                    { id: 2, label: '2 Sao' },
                    { id: 1, label: '1 Sao' },
                  ].map((item) => (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => setFilterRating(item.id)}
                      className={`px-2 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-left transition-colors cursor-pointer border ${
                        filterRating === item.id
                          ? 'bg-primary/10 text-primary border-primary/30 font-semibold'
                          : 'bg-slate-50 text-slate-600 border-slate-200 hover:bg-slate-100'
                      }`}
                    >
                      {item.label} (
                      {item.id === 'all'
                        ? reviews.length
                        : reviews.filter((r) => r.rating === item.id).length}
                      )
                    </button>
                  ))}
                </div>
              </div>

              {/* Image Toggle Section */}
              <div className="border-t border-slate-100 pt-3 flex items-center justify-between">
                <span className="text-[10px] font-semibold uppercase tracking-widest text-slate-400">
                  {t('review_filter_has_images', 'Hình ảnh')}
                </span>
                <button
                  type="button"
                  onClick={() => setFilterHasImages((prev) => !prev)}
                  className={`px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider transition-all duration-150 cursor-pointer border ${
                    filterHasImages
                      ? 'bg-primary/10 text-primary border-primary/30 font-semibold'
                      : 'bg-slate-50 text-slate-600 border-slate-200 hover:bg-slate-100'
                  }`}
                >
                  {t('review_filter_has_images', 'Có ảnh')} (
                  {reviews.filter((r) => r.images && r.images.length > 0).length})
                </button>
              </div>

              {/* Clear Filters Option */}
              {(filterRating !== 'all' || filterHasImages) && (
                <div className="border-t border-slate-100 pt-3 mt-3 flex justify-end">
                  <button
                    type="button"
                    onClick={() => {
                      setFilterRating('all');
                      setFilterHasImages(false);
                    }}
                    className="text-[9px] font-semibold uppercase tracking-widest text-red-600 hover:text-red-700 transition-colors bg-transparent border-none cursor-pointer"
                  >
                    {t('review_filter_clear', 'Xóa bộ lọc')}
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="text-xs font-semibold text-slate-500 uppercase tracking-widest">
          {filteredReviews.length} {t('review_filter_matching', 'Đánh giá phù hợp')}
        </div>
      </div>

      {/* Reviews List */}
      <div className="space-y-6">
        {isLoading ? (
          <div className="text-center py-10">
            <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-primary mx-auto mb-2"></div>
            <p className="text-xs uppercase font-semibold tracking-widest text-slate-400">
              {t('review_loading', 'Đang tải đánh giá...')}
            </p>
          </div>
        ) : filteredReviews.length === 0 ? (
          <div className="text-center py-10 border border-dashed border-slate-300">
            <AlertCircle className="mx-auto text-slate-400 mb-2" size={24} />
            <p className="text-xs uppercase font-semibold tracking-wider text-slate-400">
              {t('review_empty', 'Không tìm thấy đánh giá nào.')}
            </p>
          </div>
        ) : (
          filteredReviews.map((review) => {
            const isMine = isMyReview(review);

            return (
              <div
                key={review.id}
                className={`border-b border-slate-100 pb-6 last:border-0 last:pb-0 p-4 transition-colors ${
                  isMine ? 'bg-amber-50/40 border-l-4 border-l-primary' : ''
                }`}
              >
                <div className="flex gap-4 items-start">
                  <img
                    src={review.authorAvatar}
                    alt={review.authorName}
                    className="w-10 h-10 rounded-full object-cover border border-slate-200 bg-slate-100"
                  />
                  <div className="flex-grow text-left">
                    <div className="flex justify-between items-start gap-4">
                      <div>
                        <div className="flex items-center gap-2">
                          <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-900 mb-0.5">
                            {review.authorName}
                          </h4>
                          {isMine && (
                            <span className="text-[9px] font-black uppercase tracking-wider bg-primary/10 text-primary px-2 py-0.5 rounded">
                              Đánh giá của bạn
                            </span>
                          )}
                        </div>
                        <div className="flex gap-0.5 mb-2">
                          {[1, 2, 3, 4, 5].map((star) => (
                            <Star
                              key={star}
                              size={12}
                              className={
                                star <= review.rating ? 'fill-amber-400 text-amber-400' : 'text-slate-200'
                              }
                            />
                          ))}
                        </div>
                      </div>

                      <div className="flex items-center gap-3">
                        <span className="text-[10px] font-semibold text-slate-400">
                          {review.createdAt
                            ? new Date(review.createdAt).toLocaleDateString(
                                language === 'VN'
                                  ? 'vi-VN'
                                  : language === 'EN'
                                  ? 'en-US'
                                  : language === 'JP'
                                  ? 'ja-JP'
                                  : language === 'KR'
                                  ? 'ko-KR'
                                  : 'zh-CN',
                                {
                                  year: 'numeric',
                                  month: 'long',
                                  day: 'numeric',
                                }
                              )
                            : ''}
                        </span>

                        {/* Edit / Delete Buttons for Owner */}
                        {isMine && (
                          <div className="flex gap-2 border-l border-slate-200 pl-3">
                            <button
                              onClick={() => handleOpenEditFeedback(review)}
                              className="flex items-center gap-1 text-[10px] font-bold uppercase tracking-wider text-slate-500 hover:text-primary border-none bg-transparent cursor-pointer"
                              title="Chỉnh sửa đánh giá"
                            >
                              <Edit3 size={12} />
                              <span>Sửa</span>
                            </button>
                            <button
                              onClick={() => setDeletingFeedbackId(review.id)}
                              className="flex items-center gap-1 text-[10px] font-bold uppercase tracking-wider text-slate-500 hover:text-red-600 border-none bg-transparent cursor-pointer"
                              title="Xóa đánh giá"
                            >
                              <Trash2 size={12} />
                              <span>Xóa</span>
                            </button>
                          </div>
                        )}
                      </div>
                    </div>

                    <p className="text-xs text-slate-600 leading-relaxed font-medium whitespace-pre-line pr-2 mb-2">
                      {review.comment}
                    </p>

                    {/* Pros & Cons Section */}
                    {(review.pros || review.cons) && (
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-2 bg-slate-50 p-3 border border-slate-150 text-[11px] font-medium leading-relaxed mb-3">
                        {review.pros && (
                          <div>
                            <span className="text-green-700 font-semibold block mb-0.5 uppercase text-[9px] tracking-wider">
                              ✓ {t('review_pros', 'Ưu điểm:')}
                            </span>
                            <span className="text-slate-600">{review.pros}</span>
                          </div>
                        )}
                        {review.cons && (
                          <div>
                            <span className="text-red-700 font-semibold block mb-0.5 uppercase text-[9px] tracking-wider">
                              ✗ {t('review_cons', 'Nhược điểm:')}
                            </span>
                            <span className="text-slate-600">{review.cons}</span>
                          </div>
                        )}
                      </div>
                    )}

                    {/* Attached Images Grid */}
                    {review.images && review.images.length > 0 && (
                      <div className="flex flex-wrap gap-2.5">
                        {review.images.map((img, imgIdx) => (
                          <div
                            key={imgIdx}
                            onClick={() => openLightbox(review.images, imgIdx)}
                            className="w-20 h-20 overflow-hidden border border-slate-200 bg-slate-100 cursor-zoom-in hover:opacity-90 transition-opacity relative group"
                          >
                            <img src={img} alt="Attached review" className="w-full h-full object-cover" />
                            <div className="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors flex items-center justify-center">
                              <Image
                                className="text-white opacity-0 group-hover:opacity-100 transition-opacity"
                                size={14}
                              />
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            );
          })
        )}
      </div>

      {/* CREATE / EDIT FEEDBACK MODAL */}
      {isFeedbackOpen && (
        <div className="fixed inset-0 z-[5000] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto font-['Montserrat']">
          <div className="bg-white border-2 border-primary/20 max-w-lg w-full p-6 relative shadow-2xl animate-scale-in text-slate-800">
            {/* Close Button */}
            <button
              type="button"
              onClick={() => setIsFeedbackOpen(false)}
              className="absolute top-4 right-4 text-slate-400 hover:text-slate-700 border-none bg-transparent cursor-pointer p-1"
            >
              <X size={20} />
            </button>

            <div className="bg-primary text-white text-[9px] font-black uppercase tracking-widest px-3 py-1 absolute top-0 left-0">
              {isEditingFeedback ? 'Chỉnh sửa đánh giá dịch vụ' : 'Đánh giá trải nghiệm dịch vụ'}
            </div>

            {/* Error banner */}
            {feedbackErrorMessage && (
              <div className="mt-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs font-semibold">
                {feedbackErrorMessage}
              </div>
            )}

            <form onSubmit={handleSubmitFeedback} className="space-y-4 text-left mt-4">
              {/* Select Booking if creating and multiple eligible stays exist */}
              {!isEditingFeedback && eligibleBookings.length > 1 && (
                <div>
                  <label className="block text-[10px] font-black uppercase tracking-widest text-slate-600 mb-1.5">
                    Chọn kỳ lưu trú bạn muốn đánh giá:
                  </label>
                  <select
                    value={selectedBookingId || ''}
                    onChange={(e) => setSelectedBookingId(Number(e.target.value))}
                    className="w-full p-2.5 border border-slate-300 text-xs font-semibold focus:border-primary focus:outline-none bg-slate-50"
                  >
                    {eligibleBookings.map((b) => (
                      <option key={b.bookingId} value={b.bookingId}>
                        {b.bookingReference} (Từ {b.checkInDate} đến {b.checkOutDate})
                      </option>
                    ))}
                  </select>
                </div>
              )}

              {/* Star Rating Selection */}
              <div>
                <span className="block text-[10px] font-black uppercase tracking-widest text-slate-600 mb-1.5">
                  Mức độ hài lòng:
                </span>
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
                        className={`transition-colors duration-100 ${
                          star <= (feedbackHoveredRating || feedbackRating)
                            ? 'fill-amber-400 text-amber-400'
                            : 'text-slate-300'
                        }`}
                      />
                    </button>
                  ))}
                </div>
              </div>

              {/* Text comment */}
              <div>
                <div className="flex justify-between items-center mb-1.5">
                  <span className="block text-[10px] font-black uppercase tracking-widest text-slate-600">
                    Bình luận chi tiết:
                  </span>
                  <span className={`text-[10px] font-bold ${feedbackComment.length > 1000 ? 'text-red-600 font-extrabold' : 'text-slate-400'}`}>
                    {feedbackComment.length}/1000
                  </span>
                </div>
                <textarea
                  rows="3"
                  value={feedbackComment}
                  onChange={(e) => setFeedbackComment(e.target.value)}
                  placeholder="Hãy chia sẻ cảm nhận thực tế của bạn về chất lượng phòng và dịch vụ..."
                  className={`w-full p-3 border text-xs font-medium focus:outline-none placeholder-slate-400 leading-relaxed resize-none rounded-none bg-slate-50 ${
                    feedbackComment.length > 1000 ? 'border-red-500 focus:border-red-600' : 'border-slate-300 focus:border-primary'
                  }`}
                  required
                />
                {feedbackComment.length > 1000 && (
                  <p className="text-[10px] font-bold text-red-600 mt-1">
                    Review comment cannot exceed 1000 characters.
                  </p>
                )}
              </div>

              {/* Pros & Cons */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <span className="block text-[10px] font-black uppercase tracking-widest text-slate-600 mb-1.5">
                    ✓ Ưu điểm:
                  </span>
                  <input
                    type="text"
                    value={feedbackPros}
                    onChange={(e) => setFeedbackPros(e.target.value)}
                    placeholder="Điểm bạn thích nhất..."
                    className="w-full p-2.5 border border-slate-300 text-xs font-medium focus:border-primary focus:outline-none placeholder-slate-400 rounded-none bg-slate-50"
                  />
                </div>
                <div>
                  <span className="block text-[10px] font-black uppercase tracking-widest text-slate-600 mb-1.5">
                    ✗ Nhược điểm:
                  </span>
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
                  <span className="block text-[10px] font-black uppercase tracking-widest text-slate-600">
                    Hình ảnh đính kèm ({feedbackImages.length}/5):
                  </span>
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
                    <span>{isEditingFeedback ? 'Lưu thay đổi' : 'Gửi đánh giá'}</span>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* DELETE CONFIRMATION MODAL */}
      {deletingFeedbackId && (
        <div className="fixed inset-0 z-[5000] bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 font-['Montserrat']">
          <div className="bg-white border-2 border-red-500/20 max-w-sm w-full p-6 relative shadow-2xl text-slate-800 text-left">
            <h3 className="text-sm font-black uppercase tracking-wider text-slate-900 mb-2">
              Xóa đánh giá?
            </h3>
            <p className="text-xs text-slate-600 mb-6 font-medium leading-relaxed">
              Bạn có chắc chắn muốn xóa bài đánh giá này không? Hành động này không thể hoàn tác.
            </p>
            <div className="flex justify-end gap-2 text-[10px] font-black uppercase tracking-widest">
              <button
                type="button"
                onClick={() => setDeletingFeedbackId(null)}
                className="px-4 py-2 border border-slate-300 text-slate-700 bg-white hover:bg-slate-100 cursor-pointer"
              >
                Hủy
              </button>
              <button
                type="button"
                onClick={handleDeleteFeedback}
                disabled={isDeletingFeedback}
                className="px-4 py-2 bg-red-600 text-white hover:bg-red-700 cursor-pointer border-none flex items-center gap-1"
              >
                {isDeletingFeedback ? 'Đang xóa...' : 'Xác nhận xóa'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Lightbox Modal */}
      {lightbox.isOpen && (
        <div
          onClick={closeLightbox}
          className="fixed inset-0 bg-black/95 z-[99999] flex flex-col justify-center items-center select-none"
        >
          {/* Close button */}
          <button
            onClick={closeLightbox}
            className="absolute top-6 right-6 text-white/75 hover:text-white bg-transparent border-none cursor-pointer p-2 flex items-center justify-center"
          >
            <span className="material-symbols-outlined text-3xl font-black">close</span>
          </button>

          {/* Main Photo Display Area */}
          <div className="relative max-w-5xl max-h-[80vh] w-full flex items-center justify-center p-4">
            {lightbox.images.length > 1 && (
              <button
                onClick={prevLightboxImage}
                className="absolute left-4 w-12 h-12 bg-white/10 hover:bg-white/20 text-white rounded-full flex items-center justify-center transition-colors border-none cursor-pointer"
              >
                <span className="material-symbols-outlined text-3xl font-black">chevron_left</span>
              </button>
            )}

            <img
              src={lightbox.images[lightbox.index]}
              alt={`Lightbox item ${lightbox.index + 1}`}
              className="max-w-full max-h-[80vh] object-contain shadow-2xl animate-scale-in"
              onClick={(e) => e.stopPropagation()}
            />

            {lightbox.images.length > 1 && (
              <button
                onClick={nextLightboxImage}
                className="absolute right-4 w-12 h-12 bg-white/10 hover:bg-white/20 text-white rounded-full flex items-center justify-center transition-colors border-none cursor-pointer"
              >
                <span className="material-symbols-outlined text-3xl font-black">chevron_right</span>
              </button>
            )}
          </div>

          {/* Caption / Page Counter */}
          <div className="mt-4 text-xs font-semibold text-white/70 uppercase tracking-widest">
            {t('review_lightbox_counter', 'Ảnh')} {lightbox.index + 1} / {lightbox.images.length}
          </div>
        </div>
      )}
    </div>
  );
}
