import { useState, useEffect, useRef, useMemo } from 'react';
import { Star, AlertCircle, Filter, ChevronDown, Image, MessageSquare } from 'lucide-react';
import { getReviews } from '../../services/reviewService';
import { useLanguage } from '../../context/LanguageContext';

export default function ReviewSection({ roomId }) {
  const { t, language } = useLanguage();
  const [reviews, setReviews] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  // Filter states
  const [filterRating, setFilterRating] = useState('all'); // 'all', 5, 4, 3, 2, 1
  const [filterHasImages, setFilterHasImages] = useState(false);
  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const filterRef = useRef(null);

  useEffect(() => {
    function handleClickOutside(event) {
      if (filterRef.current && !filterRef.current.contains(event.target)) {
        setIsFilterOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Lightbox state
  const [lightbox, setLightbox] = useState({ isOpen: false, images: [], index: 0 });

  useEffect(() => {
    // Load reviews when roomId changes
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
  }, [roomId]);

  // Statistics calculation
  const totalReviews = reviews.length;
  const averageRating = totalReviews > 0
    ? (reviews.reduce((acc, curr) => acc + curr.rating, 0) / totalReviews).toFixed(1)
    : '0.0';

  const starDistribution = [5, 4, 3, 2, 1].map(stars => {
    const count = reviews.filter(r => r.rating === stars).length;
    const percentage = totalReviews > 0 ? (count / totalReviews) * 100 : 0;
    return { stars, count, percentage };
  });

  // Filtered reviews calculation
  const filteredReviews = useMemo(() => {
    return reviews.filter(review => {
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
    setLightbox(prev => ({
      ...prev,
      index: (prev.index + 1) % prev.images.length
    }));
  };

  const prevLightboxImage = (e) => {
    e.stopPropagation();
    setLightbox(prev => ({
      ...prev,
      index: (prev.index - 1 + prev.images.length) % prev.images.length
    }));
  };

  return (
    <div className="mt-12 border-t border-slate-200 pt-10 font-['Montserrat'] text-slate-800">
      <h3 className="text-xl font-semibold uppercase tracking-wider text-slate-900 mb-8 flex items-center gap-2">
        <span>{t('review_title', 'Xem Đánh Giá')}</span>
        <span className="text-sm font-semibold bg-primary/10 text-primary px-2.5 py-0.5 rounded-full">
          {totalReviews}
        </span>
      </h3>

      {/* Review Stats Header */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-8 mb-10 bg-slate-50 p-6 border border-slate-200/60">
        <div className="md:col-span-4 flex flex-col items-center justify-center text-center border-b md:border-b-0 md:border-r border-slate-200 pb-6 md:pb-0">
          <span className="text-5xl font-semibold text-slate-900 leading-none">{averageRating}</span>
          <div className="flex gap-1 my-3">
            {[1, 2, 3, 4, 5].map((star) => (
              <Star
                key={star}
                size={18}
                className={star <= Math.round(Number(averageRating)) ? "fill-amber-400 text-amber-400" : "text-slate-300"}
              />
            ))}
          </div>
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">{t('review_average_rating', 'Đánh giá trung bình')}</span>
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

      {/* Informative Banner about how to write review */}
      <div className="mb-10 bg-white border border-primary/20 p-5 flex items-start gap-4 shadow-sm">
        <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary shrink-0">
          <MessageSquare size={18} />
        </div>
        <div>
          <h4 className="text-xs font-semibold text-slate-900 uppercase tracking-wider mb-1">
            {t('review_how_to_write_title', 'Muốn chia sẻ trải nghiệm của bạn?')}
          </h4>
          <p className="text-[10px] text-slate-500 font-semibold uppercase tracking-wide leading-relaxed">
            {t('review_how_to_write_desc', 'Để đảm bảo tính xác thực, chỉ những khách hàng đã đặt phòng và hoàn tất thủ tục trả phòng (Checked Out) mới có thể gửi đánh giá dịch vụ. Hãy vào mục Lịch sử đặt phòng trong Dashboard của bạn để gửi phản hồi cho chuyến đi.')}
          </p>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="mb-6 flex justify-between items-center relative" ref={filterRef}>
        <div className="relative">
          <button
            type="button"
            onClick={() => setIsFilterOpen(prev => !prev)}
            className={`px-4 py-2 text-xs font-semibold uppercase tracking-widest border transition-all duration-200 cursor-pointer flex items-center gap-2 ${isFilterOpen || filterRating !== 'all' || filterHasImages
                ? 'bg-primary text-white border-primary shadow-sm'
                : 'bg-white text-slate-700 border-slate-300 hover:bg-slate-50'
              }`}
          >
            <Filter size={14} />
            <span>{t('review_filter_btn', 'Bộ lọc')}</span>
            {(filterRating !== 'all' || filterHasImages) && (
              <span className="w-1.5 h-1.5 rounded-full bg-white animate-pulse" />
            )}
            <ChevronDown size={14} className={`transition-transform duration-200 ${isFilterOpen ? 'rotate-180' : ''}`} />
          </button>

          {/* Dropdown Menu */}
          {isFilterOpen && (
            <div className="absolute left-0 mt-2 w-72 bg-white border border-slate-200 shadow-xl p-4 z-30 animate-scale-in font-['Montserrat']">
              {/* Star Rating Section */}
              <div className="mb-4">
                <span className="block text-[10px] font-semibold uppercase tracking-widest text-slate-400 mb-2">{t('review_filter_stars', 'Số sao')}</span>
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
                      className={`px-2 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-left transition-colors cursor-pointer border ${filterRating === item.id
                          ? 'bg-primary/10 text-primary border-primary/30 font-semibold'
                          : 'bg-slate-50 text-slate-600 border-slate-200 hover:bg-slate-100'
                        }`}
                    >
                      {item.label} ({item.id === 'all' ? reviews.length : reviews.filter(r => r.rating === item.id).length})
                    </button>
                  ))}
                </div>
              </div>

              {/* Image Toggle Section */}
              <div className="border-t border-slate-100 pt-3 flex items-center justify-between">
                <span className="text-[10px] font-semibold uppercase tracking-widest text-slate-400">{t('review_filter_has_images', 'Hình ảnh')}</span>
                <button
                  type="button"
                  onClick={() => setFilterHasImages(prev => !prev)}
                  className={`px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider transition-all duration-150 cursor-pointer border ${filterHasImages
                      ? 'bg-primary/10 text-primary border-primary/30 font-semibold'
                      : 'bg-slate-50 text-slate-600 border-slate-200 hover:bg-slate-100'
                    }`}
                >
                  {t('review_filter_has_images', 'Có ảnh')} ({reviews.filter(r => r.images && r.images.length > 0).length})
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
            <p className="text-xs uppercase font-semibold tracking-widest text-slate-400">{t('review_loading', 'Đang tải đánh giá...')}</p>
          </div>
        ) : filteredReviews.length === 0 ? (
          <div className="text-center py-10 border border-dashed border-slate-300">
            <AlertCircle className="mx-auto text-slate-400 mb-2" size={24} />
            <p className="text-xs uppercase font-semibold tracking-wider text-slate-400">{t('review_empty', 'Không tìm thấy đánh giá nào.')}</p>
          </div>
        ) : (
          filteredReviews.map((review) => (
            <div key={review.id} className="border-b border-slate-100 pb-6 last:border-0 last:pb-0">
              <div className="flex gap-4 items-start">
                <img
                  src={review.authorAvatar}
                  alt={review.authorName}
                  className="w-10 h-10 rounded-full object-cover border border-slate-200 bg-slate-100"
                />
                <div className="flex-grow text-left">
                  <div className="flex justify-between items-start gap-4">
                    <div>
                      <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-900 mb-0.5">{review.authorName}</h4>
                      <div className="flex gap-0.5 mb-2">
                        {[1, 2, 3, 4, 5].map((star) => (
                          <Star
                            key={star}
                            size={12}
                            className={star <= review.rating ? "fill-amber-400 text-amber-400" : "text-slate-200"}
                          />
                        ))}
                      </div>
                    </div>

                    <span className="text-[10px] font-semibold text-slate-400">
                      {review.createdAt ? new Date(review.createdAt).toLocaleDateString(
                        language === 'VN' ? 'vi-VN' : language === 'EN' ? 'en-US' : language === 'JP' ? 'ja-JP' : language === 'KR' ? 'ko-KR' : 'zh-CN',
                        {
                          year: 'numeric',
                          month: 'long',
                          day: 'numeric'
                        }
                      ) : ''}
                    </span>
                  </div>

                  <p className="text-xs text-slate-600 leading-relaxed font-medium whitespace-pre-line pr-2 mb-2">
                    {review.comment}
                  </p>

                  {/* Pros & Cons Section */}
                  {(review.pros || review.cons) && (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-2 bg-slate-50 p-3 border border-slate-150 text-[11px] font-medium leading-relaxed mb-3">
                      {review.pros && (
                        <div>
                          <span className="text-green-700 font-semibold block mb-0.5 uppercase text-[9px] tracking-wider">✓ {t('review_pros', 'Ưu điểm:')}</span>
                          <span className="text-slate-600">{review.pros}</span>
                        </div>
                      )}
                      {review.cons && (
                        <div>
                          <span className="text-red-700 font-semibold block mb-0.5 uppercase text-[9px] tracking-wider">✗ {t('review_cons', 'Nhược điểm:')}</span>
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
                            <Image className="text-white opacity-0 group-hover:opacity-100 transition-opacity" size={14} />
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))
        )}
      </div>

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
