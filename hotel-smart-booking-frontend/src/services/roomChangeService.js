import axiosInstance from './axiosInstance';

/**
 * Gọi API chuyển phòng cho khách đang check-in.
 * POST /api/room-change
 *
 * @param {object} payload
 * @param {number} payload.bookingId  - ID của booking đang Checked-in
 * @param {number} payload.newRoomId  - ID phòng muốn chuyển đến
 * @param {string} [payload.reason]   - Lý do chuyển phòng (tuỳ chọn)
 * @returns {Promise<object>} ApiResponse<RoomChangeResponse>
 */
export const changeRoom = async (payload) => {
  const response = await axiosInstance.post('/stay-adjustments/room-change', payload);
  return response.data;
};

/**
 * Lấy danh sách phòng đang trống (Available) để nhân viên chọn khi chuyển phòng.
 * GET /api/rooms?status=Available
 *
 * @returns {Promise<object>} ApiResponse chứa danh sách phòng Available
 */
export const getAvailableRooms = async () => {
  const response = await axiosInstance.get('/rooms', {
    params: { status: 'Available' },
  });
  return response.data;
};

/**
 * Khách hàng gửi yêu cầu chuyển phòng.
 * POST /api/room-change/customer/request
 */
export const submitCustomerRoomChangeRequest = async (payload) => {
  const response = await axiosInstance.post('/stay-adjustments/room-change/request', payload);
  return response.data;
};

/**
 * Nhân viên lấy danh sách các yêu cầu chuyển phòng đang chờ duyệt.
 * GET /api/room-change/pending
 */
export const getPendingRoomChangeRequests = async () => {
  const response = await axiosInstance.get('/stay-adjustments/room-change/pending');
  return response.data;
};

/**
 * Nhân viên duyệt yêu cầu chuyển phòng của khách.
 * POST /api/room-change/approve/{requestId}
 */
export const approveRoomChangeRequest = async (requestId, newRoomId) => {
  const response = await axiosInstance.post(`/stay-adjustments/room-change/approve/${requestId}?newRoomId=${newRoomId}`);
  return response.data;
};

/**
 * Nhân viên từ chối yêu cầu chuyển phòng của khách.
 * POST /api/room-change/reject/{requestId}
 */
export const rejectRoomChangeRequest = async (requestId, reason) => {
  const response = await axiosInstance.post(`/stay-adjustments/room-change/reject/${requestId}`, reason, {
    headers: { 'Content-Type': 'text/plain' }
  });
  return response.data;
};
