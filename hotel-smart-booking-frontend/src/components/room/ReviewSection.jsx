import { useState, useEffect, useRef, useMemo } from 'react';
import { Star, Trash2, Edit3, Camera, X, Image, AlertCircle, Check, Filter, ChevronDown } from 'lucide-react';
import { getReviews, createReview, updateReview, deleteReview } from '../../services/reviewService';

export default function ReviewSection({ roomId, showToast }) {
  const [reviews, setReviews] = useState([]);
  const [rating, setRating] = useState(5);
  const [hoveredRating, setHoveredRating] = useState(0);
  const [comment, setComment] = useState('');
  const [images, setImages] = useState([]); // Array of Base64 strings
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  // CRUD states
  const [editingId, setEditingId] = useState(null);
  const [editRating, setEditRating] = useState(5);
  const [editComment, setEditComment] = useState('');
  const [editImages, setEditImages] = useState([]);
  const [deletingId, setDeletingId] = useState(null);

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

  // Current logged in user info
  const [currentUser, setCurrentUser] = useState(null);

  useEffect(() => {
    // Load reviews when roomId changes
    if (roomId) {
      setReviews(getReviews(roomId));
    }

    // Get current user details if logged in
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        setCurrentUser(JSON.parse(userStr));
      } catch (e) {
        console.error('Error parsing user data', e);
      }
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

  // Convert files to Base64 utility
  const handleImageChange = (e, target = 'create') => {
    const files = Array.from(e.target.files);
    if (!files.length) return;

    // Limit to 5 images max
    const currentImagesCount = target === 'create' ? images.length : editImages.length;
    if (currentImagesCount + files.length > 5) {
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
        if (target === 'create') {
          setImages(prev => [...prev, ...base64s]);
        } else {
          setEditImages(prev => [...prev, ...base64s]);
        }
      })
      .catch(err => {
        console.error('Error reading images:', err);
        showToast('Lỗi khi tải ảnh lên. Vui lòng thử lại.', 'error');
      });
  };

  const removeImage = (index, target = 'create') => {
    if (target === 'create') {
      setImages(prev => prev.filter((_, idx) => idx !== index));
    } else {
      setEditImages(prev => prev.filter((_, idx) => idx !== index));
    }
  };

  // CREATE Review
  const handleSubmitReview = (e) => {
    e.preventDefault();
    if (!comment.trim()) {
      showToast('Vui lòng nhập bình luận đánh giá của bạn.', 'error');
      return;
    }

    setIsSubmitting(true);

    const authorName = currentUser?.fullName || currentUser?.name || 'Hội viên Elysian';
    const authorAvatar = currentUser?.avatar || currentUser?.avatarUrl || `https://i.pravatar.cc/150?img=${Math.floor(Math.random() * 50) + 1}`;

    setTimeout(() => {
      const newReview = createReview(roomId, {
        authorName,
        authorAvatar,
        rating,
        comment,
        images
      });

      if (newReview) {
        setReviews(getReviews(roomId));
        setComment('');
        setImages([]);
        setRating(5);
        showToast('Đăng đánh giá thành công! Cảm ơn ý kiến của bạn.', 'success');
      } else {
        showToast('Có lỗi xảy ra khi gửi đánh giá.', 'error');
      }
      setIsSubmitting(false);
    }, 400);
  };

  // START EDITING Review
  const startEdit = (review) => {
    setEditingId(review.id);
    setEditRating(review.rating);
    setEditComment(review.comment);
    setEditImages(review.images || []);
  };

  // UPDATE Review
  const handleUpdateReview = (e, reviewId) => {
    e.preventDefault();
    if (!editComment.trim()) {
      showToast('Vui lòng nhập bình luận đánh giá.', 'error');
      return;
    }

    const updated = updateReview(roomId, reviewId, {
      rating: editRating,
      comment: editComment,
      images: editImages
    });

    if (updated) {
      setReviews(getReviews(roomId));
      setEditingId(null);
      showToast('Cập nhật đánh giá thành công!', 'success');
    } else {
      showToast('Có lỗi xảy ra khi cập nhật đánh giá.', 'error');
    }
  };

  // DELETE Review
  const handleDeleteConfirm = (reviewId) => {
    const success = deleteReview(roomId, reviewId);
    if (success) {
      setReviews(getReviews(roomId));
      setDeletingId(null);
      showToast('Đã xóa đánh giá của bạn.', 'success');
    } else {
      showToast('Có lỗi xảy ra khi xóa đánh giá.', 'error');
    }
  };

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
      <h3 className="text-xl font-black uppercase tracking-wider text-slate-900 mb-8 flex items-center gap-2">
        <span>Xem Đánh Giá</span>
        <span className="text-sm font-bold bg-primary/10 text-primary px-2.5 py-0.5 rounded-full">
          {totalReviews}
        </span>
      </h3>

      {/* Review Stats Header */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-8 mb-10 bg-slate-50 p-6 border border-slate-200/60">
        <div className="md:col-span-4 flex flex-col items-center justify-center text-center border-b md:border-b-0 md:border-r border-slate-200 pb-6 md:pb-0">
          <span className="text-5xl font-black text-slate-900 leading-none">{averageRating}</span>
          <div className="flex gap-1 my-3">
            {[1, 2, 3, 4, 5].map((star) => (
              <Star
                key={star}
                size={18}
                className={star <= Math.round(Number(averageRating)) ? "fill-amber-400 text-amber-400" : "text-slate-300"}
              />
            ))}
          </div>
          <span className="text-xs font-bold text-slate-500 uppercase tracking-wider">Đánh giá trung bình</span>
        </div>

        <div className="md:col-span-8 flex flex-col justify-center space-y-2">
          {starDistribution.map(({ stars, count, percentage }) => (
            <div key={stars} className="flex items-center text-xs font-bold text-slate-600 gap-3">
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

      {/* Add Review Form */}
      <form onSubmit={handleSubmitReview} className="mb-12 bg-white border-2 border-primary/20 p-6 relative">
        <div className="absolute top-0 left-0 bg-primary text-white text-[9px] font-black uppercase tracking-widest px-3 py-1">
          Viết đánh giá của bạn
        </div>
        
        <div className="mt-4 mb-5 flex flex-col sm:flex-row sm:items-center gap-4 justify-between">
          <div className="flex items-center gap-3">
            <span className="text-xs font-black uppercase tracking-wider text-slate-700">Đánh giá bằng sao:</span>
            <div className="flex gap-1.5">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  type="button"
                  key={star}
                  onClick={() => setRating(star)}
                  onMouseEnter={() => setHoveredRating(star)}
                  onMouseLeave={() => setHoveredRating(0)}
                  className="p-1 cursor-pointer transition-transform hover:scale-110 focus:outline-none"
                >
                  <Star
                    size={22}
                    className={`transition-colors duration-100 ${
                      star <= (hoveredRating || rating)
                        ? "fill-amber-400 text-amber-400"
                        : "text-slate-300"
                    }`}
                  />
                </button>
              ))}
            </div>
          </div>
          
          <label className="flex items-center gap-2 cursor-pointer bg-slate-100 hover:bg-slate-200 text-slate-700 px-4 py-2 text-xs font-bold uppercase tracking-wider transition-all self-start sm:self-auto border border-slate-300">
            <Camera size={16} />
            <span>Đính kèm ảnh ({images.length}/5)</span>
            <input
              type="file"
              multiple
              accept="image/*"
              className="hidden"
              onChange={(e) => handleImageChange(e, 'create')}
            />
          </label>
        </div>

        {/* Selected Images Previews */}
        {images.length > 0 && (
          <div className="flex flex-wrap gap-3 mb-5 p-3 bg-slate-50 border border-slate-200">
            {images.map((img, idx) => (
              <div key={idx} className="relative w-16 h-16 border border-slate-300 bg-slate-200 group">
                <img src={img} alt="Preview" className="w-full h-full object-cover" />
                <button
                  type="button"
                  onClick={() => removeImage(idx, 'create')}
                  className="absolute -top-1.5 -right-1.5 bg-red-600 text-white rounded-full p-0.5 hover:bg-red-700 transition-colors shadow"
                >
                  <X size={10} />
                </button>
              </div>
            ))}
          </div>
        )}

        <div className="mb-4">
          <textarea
            rows="3"
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="Hãy chia sẻ trải nghiệm thực tế của bạn tại phòng nghỉ này..."
            className="w-full p-4 border border-slate-300 text-xs font-medium focus:border-primary focus:outline-none placeholder-slate-400 leading-relaxed resize-none rounded-none"
            required
          />
        </div>

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={isSubmitting}
            className="px-6 py-2.5 bg-primary text-white text-[10px] font-black uppercase tracking-widest hover:bg-opacity-95 transition-all cursor-pointer border-none flex items-center gap-2"
          >
            {isSubmitting ? (
              <>
                <div className="animate-spin rounded-full h-3.5 w-3.5 border-t-2 border-white" />
                <span>Đang gửi...</span>
              </>
            ) : (
              <span>ĐĂNG ĐÁNH GIÁ</span>
            )}
          </button>
        </div>
      </form>

      {/* Filter Bar */}
      <div className="mb-6 flex justify-between items-center relative" ref={filterRef}>
        <div className="relative">
          <button
            type="button"
            onClick={() => setIsFilterOpen(prev => !prev)}
            className={`px-4 py-2 text-xs font-black uppercase tracking-widest border transition-all duration-200 cursor-pointer flex items-center gap-2 ${
              isFilterOpen || filterRating !== 'all' || filterHasImages
                ? 'bg-primary text-white border-primary shadow-sm'
                : 'bg-white text-slate-700 border-slate-300 hover:bg-slate-50'
            }`}
          >
            <Filter size={14} />
            <span>Bộ lọc</span>
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
                <span className="block text-[10px] font-black uppercase tracking-widest text-slate-400 mb-2">Số sao</span>
                <div className="grid grid-cols-2 gap-1.5">
                  {[
                    { id: 'all', label: 'Tất cả' },
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
                      className={`px-2 py-1.5 text-[10px] font-bold uppercase tracking-wider text-left transition-colors cursor-pointer border ${
                        filterRating === item.id
                          ? 'bg-primary/10 text-primary border-primary/30 font-black'
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
                <span className="text-[10px] font-black uppercase tracking-widest text-slate-400">Hình ảnh</span>
                <button
                  type="button"
                  onClick={() => setFilterHasImages(prev => !prev)}
                  className={`px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider transition-all duration-150 cursor-pointer border ${
                    filterHasImages
                      ? 'bg-primary/10 text-primary border-primary/30 font-black'
                      : 'bg-slate-50 text-slate-600 border-slate-200 hover:bg-slate-100'
                  }`}
                >
                  Có ảnh ({reviews.filter(r => r.images && r.images.length > 0).length})
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
                    className="text-[9px] font-black uppercase tracking-widest text-red-600 hover:text-red-700 transition-colors bg-transparent border-none cursor-pointer"
                  >
                    Xóa bộ lọc
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="text-xs font-bold text-slate-500 uppercase tracking-widest">
          {filteredReviews.length} Đánh giá phù hợp
        </div>
      </div>

      {/* Reviews List */}
      <div className="space-y-6">
        {useMemo(() => {
          if (filteredReviews.length === 0) {
            return (
              <div className="text-center py-10 border border-dashed border-slate-300">
                <AlertCircle className="mx-auto text-slate-400 mb-2" size={24} />
                <p className="text-xs uppercase font-bold tracking-wider text-slate-400">Không tìm thấy đánh giá nào khớp với bộ lọc.</p>
              </div>
            );
          }
          return filteredReviews.map((review) => (
            <div key={review.id} className="border-b border-slate-100 pb-6 last:border-0 last:pb-0">
              
              {editingId === review.id ? (
                /* INLINE EDIT FORM */
                <form onSubmit={(e) => handleUpdateReview(e, review.id)} className="bg-slate-50 p-5 border border-slate-300 space-y-4">
                  <div className="text-[10px] font-black uppercase tracking-widest text-primary">Chỉnh sửa đánh giá của bạn</div>
                  
                  <div className="flex flex-wrap gap-4 items-center justify-between">
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-slate-700">Sao:</span>
                      <div className="flex">
                        {[1, 2, 3, 4, 5].map((star) => (
                          <button
                            type="button"
                            key={star}
                            onClick={() => setEditRating(star)}
                            className="p-0.5 cursor-pointer focus:outline-none"
                          >
                            <Star
                              size={18}
                              className={star <= editRating ? "fill-amber-400 text-amber-400" : "text-slate-300"}
                            />
                          </button>
                        ))}
                      </div>
                    </div>

                    <label className="flex items-center gap-1.5 cursor-pointer bg-white border border-slate-300 px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider hover:bg-slate-50 transition-colors">
                      <Camera size={14} />
                      <span>Thêm ảnh ({editImages.length}/5)</span>
                      <input
                        type="file"
                        multiple
                        accept="image/*"
                        className="hidden"
                        onChange={(e) => handleImageChange(e, 'edit')}
                      />
                    </label>
                  </div>

                  {/* Editing Images Previews */}
                  {editImages.length > 0 && (
                    <div className="flex flex-wrap gap-2.5 p-2.5 bg-white border border-slate-200">
                      {editImages.map((img, idx) => (
                        <div key={idx} className="relative w-14 h-14 border border-slate-200 bg-slate-100">
                          <img src={img} alt="Preview" className="w-full h-full object-cover" />
                          <button
                            type="button"
                            onClick={() => removeImage(idx, 'edit')}
                            className="absolute -top-1.5 -right-1.5 bg-red-600 text-white rounded-full p-0.5 hover:bg-red-700 transition-colors shadow"
                          >
                            <X size={8} />
                          </button>
                        </div>
                      ))}
                    </div>
                  )}

                  <textarea
                    rows="3"
                    value={editComment}
                    onChange={(e) => setEditComment(e.target.value)}
                    className="w-full p-3 border border-slate-300 text-xs font-medium focus:border-primary focus:outline-none bg-white resize-none rounded-none"
                    required
                  />

                  <div className="flex justify-end gap-2 text-[10px] font-bold uppercase tracking-widest">
                    <button
                      type="button"
                      onClick={() => setEditingId(null)}
                      className="px-4 py-2 border border-slate-300 text-slate-700 bg-white hover:bg-slate-100 transition-all cursor-pointer"
                    >
                      HỦY
                    </button>
                    <button
                      type="submit"
                      className="px-4 py-2 bg-primary text-white hover:bg-opacity-95 transition-all cursor-pointer border-none"
                    >
                      LƯU THAY ĐỔI
                    </button>
                  </div>
                </form>
              ) : (
                /* STANDARD REVIEW DETAIL */
                <div className="flex gap-4 items-start">
                  <img
                    src={review.authorAvatar}
                    alt={review.authorName}
                    className="w-10 h-10 rounded-full object-cover border border-slate-200 bg-slate-100"
                  />
                  <div className="flex-grow text-left">
                    <div className="flex justify-between items-start gap-4">
                      <div>
                        <h4 className="text-xs font-extrabold uppercase tracking-wider text-slate-900 mb-0.5">{review.authorName}</h4>
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
                      
                      <div className="flex flex-col items-end gap-1.5">
                        <span className="text-[10px] font-bold text-slate-400">
                          {new Date(review.createdAt).toLocaleDateString('vi-VN', {
                            year: 'numeric',
                            month: 'long',
                            day: 'numeric'
                          })}
                        </span>
                        
                        {/* Actions buttons */}
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => startEdit(review)}
                            className="text-slate-400 hover:text-primary p-1 transition-colors bg-transparent border-none cursor-pointer flex items-center gap-0.5"
                            title="Chỉnh sửa bình luận"
                          >
                            <Edit3 size={12} />
                            <span className="text-[9px] font-bold uppercase tracking-wider hidden sm:inline">Sửa</span>
                          </button>
                          
                          {deletingId === review.id ? (
                            <div className="flex items-center gap-1.5 bg-red-50 border border-red-200 px-2 py-0.5 shadow-sm scale-95 origin-right transition-all">
                              <span className="text-[8px] font-bold text-red-700 uppercase tracking-wider">Xác nhận?</span>
                              <button
                                onClick={() => handleDeleteConfirm(review.id)}
                                className="text-red-700 hover:text-red-900 font-extrabold text-[9px] bg-transparent border-none cursor-pointer px-1 py-0.5"
                              >
                                Có
                              </button>
                              <button
                                onClick={() => setDeletingId(null)}
                                className="text-slate-500 hover:text-slate-800 font-extrabold text-[9px] bg-transparent border-none cursor-pointer px-1 py-0.5"
                              >
                                Không
                              </button>
                            </div>
                          ) : (
                            <button
                              onClick={() => setDeletingId(review.id)}
                              className="text-slate-400 hover:text-red-600 p-1 transition-colors bg-transparent border-none cursor-pointer flex items-center gap-0.5"
                              title="Xóa bình luận"
                            >
                              <Trash2 size={12} />
                              <span className="text-[9px] font-bold uppercase tracking-wider hidden sm:inline">Xóa</span>
                            </button>
                          )}
                        </div>
                      </div>
                    </div>

                    <p className="text-xs text-slate-600 leading-relaxed font-medium whitespace-pre-line pr-2">
                      {review.comment}
                    </p>

                    {/* Attached Images Grid */}
                    {review.images && review.images.length > 0 && (
                      <div className="flex flex-wrap gap-2.5 mt-3.5">
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
              )}
            </div>
          ));
        }, [filteredReviews, editingId, editRating, editComment, editImages, deletingId])}
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
            <X size={28} />
          </button>

          {/* Main Photo Display Area */}
          <div className="relative max-w-5xl max-h-[80vh] w-full flex items-center justify-center p-4">
            
            {lightbox.images.length > 1 && (
              <button
                onClick={prevLightboxImage}
                className="absolute left-4 w-12 h-12 bg-white/10 hover:bg-white/20 text-white rounded-full flex items-center justify-center transition-colors border-none cursor-pointer"
              >
                <X className="rotate-90 text-white" size={20} style={{ transform: 'rotate(-90deg)' }} />
                <span className="material-symbols-outlined text-3xl font-black">chevron_left</span>
              </button>
            )}

            <img 
              src={lightbox.images[lightbox.index]} 
              alt={`Lightbox item ${lightbox.index + 1}`}
              className="max-w-full max-h-[80vh] object-contain shadow-2xl animate-scale-in"
              onClick={(e) => e.stopPropagation()} // Prevents closing lightbox when clicking image
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
          <div className="mt-4 text-xs font-bold text-white/70 uppercase tracking-widest">
            Ảnh {lightbox.index + 1} / {lightbox.images.length}
          </div>
        </div>
      )}
    </div>
  );
}
