import axiosInstance from './axiosInstance';

/**
 * Khách hàng gửi yêu cầu check-out sớm
 */
export const submitEarlyCheckOutRequest = async (payload) => {
  const response = await axiosInstance.post('/early-checkout/request', payload);
  return response.data;
};

/**
 * Nhân viên phê duyệt yêu cầu check-out sớm
 */
export const approveEarlyCheckOutRequest = async (requestId) => {
  const response = await axiosInstance.post(`/early-checkout/approve/${requestId}`);
  return response.data;
};

/**
 * Nhân viên từ chối yêu cầu check-out sớm
 */
export const rejectEarlyCheckOutRequest = async (requestId, rejectionReason) => {
  const response = await axiosInstance.post(`/early-checkout/reject/${requestId}`, null, {
    params: { rejectionReason }
  });
  return response.data;
};

/**
 * Lấy danh sách yêu cầu check-out sớm đang chờ phê duyệt
 */
export const getPendingEarlyCheckOutRequests = async () => {
  const response = await axiosInstance.get('/early-checkout/pending');
  return response.data;
};
