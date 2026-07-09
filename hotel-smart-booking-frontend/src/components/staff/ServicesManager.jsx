import { useState, useEffect } from 'react';
import { getServices, createService, updateService, deleteService } from '../../services/serviceService';

const ServicesManager = ({ showToast, triggerCustomConfirm }) => {
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingService, setEditingService] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    price: '',
    unit: 'Lượt',
    isactive: true,
  });

  const fetchServices = async () => {
    setLoading(true);
    try {
      const response = await getServices();
      if (response && response.success && response.data) {
        setServices(response.data);
      } else {
        setServices([]);
      }
    } catch (err) {
      console.error('Lỗi khi tải dịch vụ:', err);
      showToast('Không thể tải danh sách dịch vụ', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchServices();
  }, []);

  const handleOpenAdd = () => {
    setEditingService(null);
    setFormData({
      name: '',
      description: '',
      price: '',
      unit: 'Lượt',
      isactive: true,
    });
    setIsFormOpen(true);
  };

  const handleOpenEdit = (svc) => {
    setEditingService(svc);
    setFormData({
      name: svc.name,
      description: svc.description || '',
      price: svc.price ? svc.price.toString() : '',
      unit: svc.unit || 'Lượt',
      isactive: svc.isactive !== false,
    });
    setIsFormOpen(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      showToast('Tên dịch vụ không được để trống', 'warning');
      return;
    }
    if (!formData.price || parseFloat(formData.price) < 0) {
      showToast('Giá dịch vụ không hợp lệ', 'warning');
      return;
    }
    if (!formData.unit.trim()) {
      showToast('Đơn vị tính không được để trống', 'warning');
      return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        name: formData.name.trim(),
        description: formData.description.trim(),
        price: parseFloat(formData.price),
        unit: formData.unit.trim(),
        isactive: formData.isactive,
      };

      let response;
      if (editingService) {
        response = await updateService(editingService.id, payload);
        if (response && response.success) {
          showToast('Cập nhật dịch vụ thành công!', 'success');
        }
      } else {
        response = await createService(payload);
        if (response && response.success) {
          showToast('Thêm dịch vụ thành công!', 'success');
        }
      }
      setIsFormOpen(false);
      fetchServices();
    } catch (err) {
      console.error(err);
      showToast(err.response?.data?.message || 'Có lỗi xảy ra khi lưu thông tin.', 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = (id, name) => {
    triggerCustomConfirm(
      'XÁC NHẬN XÓA DỊCH VỤ',
      `Bạn có chắc chắn muốn xóa dịch vụ "${name}" không?`,
      async () => {
        try {
          await deleteService(id);
          showToast('Xóa dịch vụ thành công!', 'success');
          fetchServices();
        } catch (err) {
          console.error(err);
          showToast(err.response?.data?.message || 'Không thể xóa dịch vụ này.', 'error');
        }
      },
      'Xóa'
    );
  };

  return (
    <div className="space-y-6 animate-scale-in text-left">
      <div className="flex justify-between items-end border-b border-slate-200 pb-4">
        <div>
          <h3 className="text-slate-800 font-black text-base uppercase tracking-wider m-0">QUẢN LÝ DỊCH VỤ</h3>
          <p className="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">Cấu hình danh mục dịch vụ phát sinh và phụ thu (Spa, minibar, giặt là...)</p>
        </div>

        <button
          onClick={handleOpenAdd}
          className="bg-primary text-white font-black text-xs uppercase tracking-widest px-6 py-3 border-none cursor-pointer hover:brightness-110 active:scale-98 transition-all flex items-center gap-1.5"
        >
          Thêm dịch vụ
        </button>
      </div>

      {/* Service Form Overlay */}
      {isFormOpen && (
        <div className="bg-white border border-slate-200/80 p-6 md:p-8 shadow-xl">
          <h4 className="text-xs font-black text-slate-800 uppercase tracking-widest border-b border-slate-100 pb-3 mb-6">
            {editingService ? `CHỈNH SỬA DỊCH VỤ: ${editingService.name}` : 'THÊM MỚI DỊCH VỤ'}
          </h4>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">Tên dịch vụ</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
                  placeholder="E.g., Giặt ủi cao cấp"
                  className="w-full bg-transparent border-b border-slate-200 py-2 font-bold text-xs outline-none text-slate-800 focus:border-primary placeholder:text-slate-400"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">Đơn giá (VND)</label>
                <input
                  type="number"
                  value={formData.price}
                  onChange={(e) => setFormData(prev => ({ ...prev, price: e.target.value }))}
                  placeholder="E.g., 150000"
                  className="w-full bg-transparent border-b border-slate-200 py-2 font-bold text-xs outline-none text-slate-800 focus:border-primary placeholder:text-slate-400"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">Đơn vị tính</label>
                <input
                  type="text"
                  value={formData.unit}
                  onChange={(e) => setFormData(prev => ({ ...prev, unit: e.target.value }))}
                  placeholder="E.g., Lượt, Chiếc, Lon..."
                  className="w-full bg-transparent border-b border-slate-200 py-2 font-bold text-xs outline-none text-slate-800 focus:border-primary placeholder:text-slate-400"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">Trạng thái hoạt động</label>
                <select
                  value={formData.isactive ? 'true' : 'false'}
                  onChange={(e) => setFormData(prev => ({ ...prev, isactive: e.target.value === 'true' }))}
                  className="w-full bg-transparent border-b border-slate-200 py-2 font-bold text-xs outline-none text-slate-800 focus:border-primary [&>option]:bg-white [&>option]:text-slate-800"
                >
                  <option value="true">Hoạt động (Active)</option>
                  <option value="false">Tạm ngưng (Inactive)</option>
                </select>
              </div>
            </div>

            <div className="space-y-2">
              <label className="block text-[10px] font-bold text-slate-500 uppercase tracking-widest">Mô tả dịch vụ</label>
              <textarea
                rows="3"
                value={formData.description}
                onChange={(e) => setFormData(prev => ({ ...prev, description: e.target.value }))}
                placeholder="Nhập mô tả chi tiết dịch vụ..."
                className="w-full bg-transparent border border-slate-200 p-3 font-bold text-xs outline-none text-slate-800 focus:border-primary resize-none placeholder:text-slate-400"
              />
            </div>

            <div className="flex gap-4">
              <button
                type="submit"
                disabled={isSubmitting}
                className="bg-primary text-white font-bold px-8 py-3.5 uppercase text-xs tracking-widest hover:brightness-110 active:scale-98 transition-all cursor-pointer border-none flex items-center justify-center gap-1.5"
              >
                {isSubmitting ? 'Đang lưu...' : 'Lưu lại'}
              </button>
              <button
                type="button"
                onClick={() => setIsFormOpen(false)}
                className="bg-slate-50 hover:bg-slate-100 text-slate-800 font-bold px-8 py-3.5 uppercase text-xs tracking-widest border border-slate-200 cursor-pointer"
              >
                Hủy bỏ
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Services Table */}
      <div className="bg-white border border-slate-200/85 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-slate-500 font-bold uppercase tracking-widest text-xs">
            Đang tải danh sách dịch vụ...
          </div>
        ) : services.length === 0 ? (
          <div className="p-12 text-center text-slate-500 font-bold uppercase tracking-widest text-xs">
            Chưa có dịch vụ nào trong hệ thống
          </div>
        ) : (
          <table className="w-full border-collapse text-left text-xs text-slate-650">
            <thead>
              <tr className="border-b border-slate-100 text-[9px] font-black uppercase tracking-wider text-slate-500">
                <th className="p-4 pl-6">ID</th>
                <th className="p-4">Tên dịch vụ</th>
                <th className="p-4">Mô tả</th>
                <th className="p-4">Đơn giá</th>
                <th className="p-4">Đơn vị tính</th>
                <th className="p-4">Trạng thái</th>
                <th className="p-4 pr-6 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {services.map((svc) => (
                <tr key={svc.id} className="hover:bg-slate-50/50 transition-colors">
                  <td className="p-4 pl-6 text-slate-400 font-bold">
                    #{svc.id}
                  </td>
                  <td className="p-4">
                    <span className="text-slate-800 font-black uppercase text-sm block">{svc.name}</span>
                  </td>
                  <td className="p-4 max-w-xs truncate font-medium text-slate-500">
                    {svc.description || <span className="text-slate-400 italic">Không có mô tả</span>}
                  </td>
                  <td className="p-4 text-primary font-black text-sm">
                    {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(svc.price || 0)}
                  </td>
                  <td className="p-4 font-semibold text-slate-500">
                    {svc.unit}
                  </td>
                  <td className="p-4">
                    <span className={`px-2 py-0.5 text-[8px] font-black uppercase tracking-widest ${
                      svc.isactive 
                        ? 'bg-green-50 text-green-700 border border-green-200' 
                        : 'bg-red-50 text-red-600 border border-red-200'
                    }`}>
                      {svc.isactive ? 'Hoạt động' : 'Tạm ngưng'}
                    </span>
                  </td>
                  <td className="p-4 pr-6 text-right">
                    <div className="flex justify-end gap-2">
                      <button
                        onClick={() => handleOpenEdit(svc)}
                        className="bg-slate-100 border border-slate-200 text-slate-700 hover:bg-slate-200 hover:text-slate-900 text-[8.5px] font-black uppercase tracking-widest px-2.5 py-1.5 cursor-pointer rounded-sm transition-all"
                      >
                        Sửa
                      </button>
                      <button
                        onClick={() => handleDelete(svc.id, svc.name)}
                        className="bg-rose-50 border border-rose-200 text-rose-700 hover:bg-rose-100 hover:border-rose-350 text-[8.5px] font-black uppercase tracking-widest px-2.5 py-1.5 cursor-pointer rounded-sm transition-all"
                      >
                        Xóa
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default ServicesManager;
