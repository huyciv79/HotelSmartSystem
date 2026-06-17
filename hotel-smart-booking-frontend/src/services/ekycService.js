import axiosInstance from './axiosInstance';

/**
 * Submit eKYC verification with front ID, back ID, and selfie images.
 * Số CCCD sẽ được AI tự động đọc từ ảnh mặt trước bằng OCR.
 * @param {File} frontImage  - Ảnh mặt trước CCCD
 * @param {File} backImage   - Ảnh mặt sau CCCD
 * @param {File} selfieImage - Ảnh selfie khuôn mặt
 * @returns {Promise<object>} API response
 */
export const submitEkyc = async (frontImage, backImage, selfieImage) => {
  const formData = new FormData();
  formData.append('frontImage', frontImage);
  formData.append('backImage', backImage);
  formData.append('selfieImage', selfieImage);

  const response = await axiosInstance.post('/v1/ekyc/verify', formData, {
    headers: { 'Content-Type': undefined },
    timeout: 120000, // 120s – OCR + AI model có thể mất thời gian
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
 * Số CCCD sẽ được AI tự động đọc từ ảnh mặt trước bằng OCR.
 */
export const updateEkyc = async (frontImage, backImage, selfieImage) => {
  const formData = new FormData();
  formData.append('frontImage', frontImage);
  formData.append('backImage', backImage);
  formData.append('selfieImage', selfieImage);

  const response = await axiosInstance.post('/v1/ekyc/update', formData, {
    headers: { 'Content-Type': undefined },
    timeout: 120000,
  });
  return response.data;
};
