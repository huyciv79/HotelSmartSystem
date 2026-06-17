import axiosInstance from './axiosInstance';

/**
 * Fetch reviews for a specific room type from the backend.
 * GET /api/feedbacks
 */
export const getReviews = async (roomTypeId) => {
  if (!roomTypeId) return [];
  try {
    const response = await axiosInstance.get(`/feedbacks?roomTypeId=${roomTypeId}&size=100`);
    if (response.data && response.data.success && response.data.data) {
      // Map backend fields to frontend expected fields if needed, 
      // or return them directly.
      // Backend: feedbackId, rating, comment, pros, cons, createdAt, customerName, customerAvatar, images
      // Frontend expects: id, authorName, authorAvatar, rating, comment, createdAt, images
      const content = response.data.data.content || [];
      return content.map(item => ({
        id: item.feedbackId,
        authorName: item.customerName || 'Hội viên Elysian',
        authorAvatar: item.customerAvatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150',
        rating: item.rating,
        comment: item.comment,
        pros: item.pros,
        cons: item.cons,
        createdAt: item.createdAt,
        images: item.images || [],
        bookingId: item.bookingId,
        bookingReference: item.bookingReference
      }));
    }
    return [];
  } catch (error) {
    console.error('Error fetching reviews:', error);
    return [];
  }
};

/**
 * Fetch feedback for a specific booking.
 * GET /api/feedbacks?bookingId={bookingId}
 */
export const getFeedbackByBookingId = async (bookingId) => {
  if (!bookingId) return null;
  try {
    const response = await axiosInstance.get(`/feedbacks?bookingId=${bookingId}`);
    if (response.data && response.data.success && response.data.data) {
      const content = response.data.data.content || [];
      if (content.length > 0) {
        const item = content[0];
        return {
          id: item.feedbackId,
          authorName: item.customerName,
          authorAvatar: item.customerAvatar,
          rating: item.rating,
          comment: item.comment,
          pros: item.pros,
          cons: item.cons,
          createdAt: item.createdAt,
          images: item.images || [],
          bookingId: item.bookingId,
          bookingReference: item.bookingReference
        };
      }
    }
    return null;
  } catch (error) {
    console.error('Error fetching booking feedback:', error);
    return null;
  }
};

/**
 * Create a new feedback in backend.
 * POST /api/feedbacks
 */
export const createReview = async (bookingId, reviewData) => {
  try {
    const payload = {
      bookingId: Number(bookingId),
      rating: Number(reviewData.rating),
      comment: reviewData.comment || '',
      pros: reviewData.pros || '',
      cons: reviewData.cons || '',
      images: reviewData.images || []
    };
    const response = await axiosInstance.post('/feedbacks', payload);
    return response.data;
  } catch (error) {
    console.error('Error creating feedback:', error);
    throw error;
  }
};

/**
 * Update an existing feedback in backend.
 * PUT /api/feedbacks/{id}
 */
export const updateReview = async (feedbackId, reviewData) => {
  try {
    const payload = {
      bookingId: Number(reviewData.bookingId),
      rating: Number(reviewData.rating),
      comment: reviewData.comment || '',
      pros: reviewData.pros || '',
      cons: reviewData.cons || '',
      images: reviewData.images || []
    };
    const response = await axiosInstance.put(`/feedbacks/${feedbackId}`, payload);
    return response.data;
  } catch (error) {
    console.error('Error updating feedback:', error);
    throw error;
  }
};

/**
 * Delete a feedback in backend.
 * DELETE /api/feedbacks/{id}
 */
export const deleteReview = async (feedbackId) => {
  try {
    const response = await axiosInstance.delete(`/feedbacks/${feedbackId}`);
    return response.data && response.data.success;
  } catch (error) {
    console.error('Error deleting feedback:', error);
    throw error;
  }
};
