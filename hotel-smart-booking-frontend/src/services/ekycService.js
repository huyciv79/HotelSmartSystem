import axiosInstance from './axiosInstance';

/**
 * Submit eKYC verification with two ID images and five liveness face frames.
 * Backend bắt buộc OCR CCCD và full liveness thành công trước khi đăng ký FaceID.
 * Ảnh CCCD dùng cho OCR/hồ sơ; năm frame tạo face template nhiều góc.
 * Không so sánh khuôn mặt selfie với chân dung trên CCCD tại bước đăng ký.
 * @param {File} frontImage  - Ảnh mặt trước CCCD
 * @param {File} backImage   - Ảnh mặt sau CCCD
 * @param {{center: File, left: File, right: File, up: File, down: File}} faceFrames
 * @returns {Promise<object>} API response
 */
const appendLivenessFrames = (formData, faceFrames) => {
  formData.append('selfieImage', faceFrames.center);
  formData.append('leftImage', faceFrames.left);
  formData.append('rightImage', faceFrames.right);
  formData.append('upImage', faceFrames.up);
  formData.append('downImage', faceFrames.down);
};

/**
 * Validate a single liveness frame in real-time.
 * @param {File} frameImage - Ảnh frame cần validate
 * @param {string} step - Tên bước (center, left, right, up, down)
 * @returns {Promise<object>} API response
 */
export const validateLivenessFrame = async (
  frameImage,
  step,
  referenceImage = null,
  oppositeImage = null,
) => {
  const formData = new FormData();
  formData.append('frameImage', frameImage);
  formData.append('step', step);
  if (referenceImage) {
    formData.append('referenceImage', referenceImage);
  }
  if (oppositeImage) {
    formData.append('oppositeImage', oppositeImage);
  }

  const response = await axiosInstance.post('/v1/ekyc/face/validate-frame', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
  });
  return response.data?.data || response.data;
};

export const submitEkyc = async (frontImage, backImage, faceFrames) => {
  const formData = new FormData();
  formData.append('frontImage', frontImage);
  formData.append('backImage', backImage);
  appendLivenessFrames(formData, faceFrames);

  const response = await axiosInstance.post('/v1/ekyc/verify', formData, {
    headers: { 'Content-Type': undefined },
    timeout: 600000, // Full OCR + 5-frame liveness có thể chậm trên CPU
  });
  return response.data;
};

/**
 * Fetch the current user's eKYC profile/status.
 * @returns {Promise<object>} API response
 */
export const getEkycProfile = async () => {
  const response = await axiosInstance.get('/v1/ekyc/status');
  return response.data;
};

/**
 * Update eKYC data (re-submit with new images).
 * Đăng ký lại face template nhiều góc và cập nhật ảnh CCCD/OCR.
 */
export const updateEkyc = async (frontImage, backImage, faceFrames) => {
  const formData = new FormData();
  formData.append('frontImage', frontImage);
  formData.append('backImage', backImage);
  appendLivenessFrames(formData, faceFrames);

  const response = await axiosInstance.post('/v1/ekyc/update', formData, {
    headers: { 'Content-Type': undefined },
    timeout: 600000,
  });
  return response.data;
};
