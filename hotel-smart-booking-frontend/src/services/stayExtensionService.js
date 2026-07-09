import axiosInstance from './axiosInstance';

export const submitStayExtensionRequest = async (requestData) => {
  const response = await axiosInstance.post('/stay-adjustments/extension/request', requestData);
  return response.data;
};

export const approveStayExtensionRequest = async (requestId) => {
  const response = await axiosInstance.post(`/stay-adjustments/extension/approve/${requestId}`);
  return response.data;
};

export const rejectStayExtensionRequest = async (requestId, rejectionReason) => {
  const response = await axiosInstance.post(`/stay-adjustments/extension/reject/${requestId}`, null, {
    params: { rejectionReason }
  });
  return response.data;
};

export const getPendingStayExtensionRequests = async () => {
  const response = await axiosInstance.get('/stay-adjustments/extension/pending');
  return response.data;
};
