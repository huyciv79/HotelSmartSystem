import axiosInstance from './axiosInstance';

export const submitRefundRequest = async (bookingId, reason) => {
  const response = await axiosInstance.post(`/bookings/${bookingId}/refund-request`, { reason });
  return response.data;
};

export const getPendingRefundRequests = async () => {
  const response = await axiosInstance.get('/refund-requests/pending');
  return response.data;
};

export const approveRefundRequest = async (requestId, refundAmount) => {
  const body = refundAmount ? { refundAmount } : {};
  const response = await axiosInstance.post(`/refund-requests/${requestId}/approve`, body);
  return response.data;
};

export const rejectRefundRequest = async (requestId, rejectionReason) => {
  const response = await axiosInstance.post(`/refund-requests/${requestId}/reject`, { rejectionReason });
  return response.data;
};
