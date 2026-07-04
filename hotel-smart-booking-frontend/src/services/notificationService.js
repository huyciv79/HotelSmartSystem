import axiosInstance from './axiosInstance';

export const getNotifications = async (unreadOnly = false, page = 0, size = 10) => {
  const response = await axiosInstance.get('/notifications', {
    params: { unreadOnly, page, size },
  });
  return response.data;
};

export const markAsRead = async (id) => {
  const response = await axiosInstance.put(`/notifications/${id}/read`);
  return response.data;
};

export const markAllAsRead = async () => {
  const response = await axiosInstance.put('/notifications/read-all');
  return response.data;
};

export const getUnreadCount = async () => {
  const response = await axiosInstance.get('/notifications/unread-count');
  return response.data;
};
