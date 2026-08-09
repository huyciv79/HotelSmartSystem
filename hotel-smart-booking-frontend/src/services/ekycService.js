import axiosInstance from './axiosInstance';

const appendLivenessFrames = (formData, faceFrames) => {
  formData.append('selfieImage', faceFrames.center);
  formData.append('leftImage', faceFrames.left);
  formData.append('rightImage', faceFrames.right);
  formData.append('upImage', faceFrames.up);
  formData.append('downImage', faceFrames.down);
};

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
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 600000,
  });
  return response.data;
};

export const getEkycProfile = async () => {
  const response = await axiosInstance.get('/v1/ekyc/status');
  return response.data;
};

// Tai tai lieu eKYC qua backend; khong bao gio tra URL Supabase cho frontend.
export const getEkycDocument = async (userId, type) => {
  return axiosInstance.get(
    `/v1/admin/users/${encodeURIComponent(userId)}/ekyc-documents/${encodeURIComponent(type)}`,
    { responseType: 'blob', timeout: 120000 },
  );
};
