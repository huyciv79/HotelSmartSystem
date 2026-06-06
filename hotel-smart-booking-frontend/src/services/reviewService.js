// Service to manage Room Reviews (CRUD operations saved to LocalStorage)

const STORAGE_KEY_PREFIX = 'elysian_room_reviews_';

// High-quality mock reviews to seed the system initially
const mockSeedReviews = {
  // We can seed reviews for various room types dynamically.
  // If a room doesn't have custom seed reviews, we fallback to general luxury reviews.
  default: [
    {
      id: 'seed-1',
      authorName: 'Nguyễn Văn Minh',
      authorAvatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150',
      rating: 5,
      comment: 'Trải nghiệm vô cùng tuyệt vời! Phòng rộng rãi, thiết kế sang trọng đẳng cấp và view nhìn ra biển rất thơ mộng. Dịch vụ chăm sóc khách hàng ở Elysian chuẩn 5 sao, nhất định tôi sẽ quay lại cùng gia đình.',
      createdAt: '2026-05-15T14:30:00.000Z',
      images: [
        'https://images.unsplash.com/photo-1582719508461-905c673771fd?w=600',
        'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=600'
      ]
    },
    {
      id: 'seed-2',
      authorName: 'Trần Thị Mai',
      authorAvatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150',
      rating: 4,
      comment: 'Phòng ốc rất sạch sẽ và đầy đủ tiện nghi hiện đại. Đồ ăn sáng ngon miệng và đa dạng món. Chỉ có điểm trừ nhỏ là thời gian check-in hơi lâu một chút do đông khách, nhưng nhân viên đã nhiệt tình xin lỗi và bù đắp bằng một đĩa hoa quả tươi.',
      createdAt: '2026-05-28T09:15:00.000Z',
      images: [
        'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=600'
      ]
    }
  ]
};

export const getReviews = (roomId) => {
  if (!roomId) return [];
  const key = `${STORAGE_KEY_PREFIX}${roomId}`;
  const stored = localStorage.getItem(key);
  
  if (!stored) {
    // Seed default reviews for this room
    const seeds = mockSeedReviews.default.map(r => ({
      ...r,
      // Randomize date slightly to make it look active
      createdAt: new Date(Date.now() - Math.random() * 10 * 24 * 60 * 60 * 1000).toISOString()
    }));
    localStorage.setItem(key, JSON.stringify(seeds));
    return seeds;
  }
  
  return JSON.parse(stored);
};

export const createReview = (roomId, reviewData) => {
  if (!roomId) return null;
  const key = `${STORAGE_KEY_PREFIX}${roomId}`;
  const reviews = getReviews(roomId);
  
  const newReview = {
    id: `rev-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    authorName: reviewData.authorName || 'Hội viên Elysian',
    authorAvatar: reviewData.authorAvatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150',
    rating: Number(reviewData.rating) || 5,
    comment: reviewData.comment || '',
    createdAt: new Date().toISOString(),
    images: reviewData.images || [] // Base64 or ObjectURLs (persisting Base64 to localStorage works best)
  };
  
  const updatedReviews = [newReview, ...reviews];
  localStorage.setItem(key, JSON.stringify(updatedReviews));
  return newReview;
};

export const updateReview = (roomId, reviewId, updatedData) => {
  if (!roomId || !reviewId) return null;
  const key = `${STORAGE_KEY_PREFIX}${roomId}`;
  const reviews = getReviews(roomId);
  
  const index = reviews.findIndex(r => r.id === reviewId);
  if (index === -1) return null;
  
  const updatedReview = {
    ...reviews[index],
    rating: Number(updatedData.rating) || reviews[index].rating,
    comment: updatedData.comment !== undefined ? updatedData.comment : reviews[index].comment,
    images: updatedData.images !== undefined ? updatedData.images : reviews[index].images,
    updatedAt: new Date().toISOString()
  };
  
  const updatedReviews = [...reviews];
  updatedReviews[index] = updatedReview;
  localStorage.setItem(key, JSON.stringify(updatedReviews));
  return updatedReview;
};

export const deleteReview = (roomId, reviewId) => {
  if (!roomId || !reviewId) return false;
  const key = `${STORAGE_KEY_PREFIX}${roomId}`;
  const reviews = getReviews(roomId);
  
  const filtered = reviews.filter(r => r.id !== reviewId);
  localStorage.setItem(key, JSON.stringify(filtered));
  return true;
};
