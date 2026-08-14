import { useState, useEffect } from 'react';
import {
  getServices,
  createService,
  updateService,
  deleteService
} from '../../services/serviceService';

const ServicesManager = ({ showToast, triggerCustomConfirm, isManager = true }) => {
  const [services, setServices] = useState([]);
  const [loading, setLoading] = useState(false);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingService, setEditingService] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    price: '',
    unit: 'Lượt',
    isactive: true,
  });

  const filteredServices = services.filter(svc => 
    svc.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    svc.id.toString().includes(searchQuery) ||
    (svc.description && svc.description.toLowerCase().includes(searchQuery.toLowerCase()))
  );

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
    setFormData({ name: '', description: '', price: '', unit: 'Lượt', isactive: true });
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
    if (!formData.name.trim()) { showToast('Tên dịch vụ không được để trống', 'warning'); return; }
    if (!formData.price || parseFloat(formData.price) < 0) { showToast('Giá dịch vụ không hợp lệ', 'warning'); return; }
    if (!formData.unit.trim()) { showToast('Đơn vị tính không được để trống', 'warning'); return; }

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
        if (response && response.success) showToast('Cập nhật dịch vụ thành công!', 'success');
      } else {
        response = await createService(payload);
        if (response && response.success) showToast('Thêm dịch vụ thành công!', 'success');
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
      'Xác nhận xóa dịch vụ',
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
      {/* Header */}
      {isFormOpen ? (
        <div className="flex justify-between items-center border-b border-slate-100 pb-4">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setIsFormOpen(false)}
              className="w-9 h-9 rounded-xl bg-slate-50 border border-slate-200/60 text-slate-600 hover:bg-slate-100 flex items-center justify-center cursor-pointer transition-all"
            >
              <span className="material-symbols-outlined text-sm font-bold">arrow_back</span>
            </button>
            <div>
              <h3 className="text-slate-800 font-black text-base uppercase tracking-wider m-0">
                {editingService ? 'Chỉnh sửa dịch vụ' : 'Thêm mới dịch vụ'}
              </h3>
              <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest mt-1">
                {editingService ? `Cấu hình thông tin cho: ${editingService.name}` : 'Nhập thông tin dịch vụ phát sinh và phụ thu'}
              </p>
            </div>
          </div>
        </div>
      ) : (
        <div className="flex flex-col md:flex-row justify-between items-start md:items-end gap-4">
          <div>
            <h3 className="text-slate-800 font-black text-base uppercase tracking-wider m-0">Quản lý dịch vụ</h3>
            <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest mt-1">Cấu hình danh mục dịch vụ phát sinh và phụ thu (Spa, minibar, giặt là...)</p>
          </div>

          <div className="flex w-full md:w-auto items-center gap-3">
            {/* Search Box */}
            <div className="relative flex-grow md:flex-grow-0 w-full md:w-64">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm select-none">
                search
              </span>
              <input
                type="text"
                placeholder="Tìm tên hoặc mô tả..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full bg-slate-50 border border-slate-200/60 rounded-xl pl-9 pr-9 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all placeholder:text-slate-400"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 bg-transparent border-none p-0 cursor-pointer flex items-center"
                >
                  <span className="material-symbols-outlined text-sm">close</span>
                </button>
              )}
            </div>

            <button
              onClick={handleOpenAdd}
              className="bg-primary hover:brightness-110 text-white font-black text-xs uppercase tracking-widest px-5 py-3 border-none cursor-pointer transition-all flex items-center gap-1.5 rounded-xl shadow-[0_4px_12px_rgba(162,5,19,0.2)] shrink-0"
            >
              <span className="material-symbols-outlined text-sm">add</span>
              Thêm dịch vụ
            </button>
          </div>
        </div>
      )}

      {/* Main Content Area */}
      {isFormOpen ? (
        <div className="bg-white border border-slate-100 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.015)] p-6 md:p-8">
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Tên dịch vụ</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="VD: Ăn sáng tại phòng, Dịch vụ giặt là..."
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-3 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all placeholder:text-slate-400"
                  required
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Giá dịch vụ (VNĐ)</label>
                <input
                  type="number"
                  min="0"
                  value={formData.price}
                  onChange={(e) => setFormData({ ...formData, price: e.target.value })}
                  placeholder="VD: 150000"
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-3 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all placeholder:text-slate-400"
                  required
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Đơn vị tính</label>
                <input
                  type="text"
                  value={formData.unit}
                  onChange={(e) => setFormData({ ...formData, unit: e.target.value })}
                  placeholder="VD: Lượt, Suất, Bộ, Khẩu phần..."
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-3 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all placeholder:text-slate-400"
                  required
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Trạng thái hoạt động</label>
                <div className="flex items-center gap-4 pt-2">
                  <label className="flex items-center gap-2 cursor-pointer text-xs font-bold text-slate-700">
                    <input
                      type="radio"
                      name="isactive"
                      checked={formData.isactive === true}
                      onChange={() => setFormData({ ...formData, isactive: true })}
                      className="accent-primary"
                    />
                    Hoạt động
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer text-xs font-bold text-slate-700">
                    <input
                      type="radio"
                      name="isactive"
                      checked={formData.isactive === false}
                      onChange={() => setFormData({ ...formData, isactive: false })}
                      className="accent-primary"
                    />
                    Tạm ngưng
                  </label>
                </div>
              </div>

              <div className="md:col-span-2 space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Mô tả dịch vụ</label>
                <textarea
                  rows="3"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Nhập ghi chú hoặc thông tin quy định đi kèm của dịch vụ..."
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl p-4 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all placeholder:text-slate-400 resize-none"
                />
              </div>
            </div>

            <div className="flex justify-end gap-3 border-t border-slate-100 pt-6">
              <button
                type="button"
                onClick={() => setIsFormOpen(false)}
                className="bg-slate-100 hover:bg-slate-200 text-slate-700 font-black text-xs uppercase tracking-widest px-6 py-3 border-none cursor-pointer transition-all rounded-xl"
              >
                Hủy
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                className="bg-primary hover:brightness-110 text-white font-black text-xs uppercase tracking-widest px-6 py-3 border-none cursor-pointer transition-all rounded-xl shadow-md"
              >
                {isSubmitting ? 'Đang lưu...' : editingService ? 'Cập nhật' : 'Tạo mới'}
              </button>
            </div>
          </form>
        </div>
      ) : (
        /* DANH MỤC DỊCH VỤ */
        <div className="bg-white border border-slate-100 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.015)] overflow-hidden">
          {loading ? (
            <div className="p-8 text-center text-xs text-slate-500 font-bold">Đang tải danh sách dịch vụ...</div>
          ) : filteredServices.length === 0 ? (
            <div className="p-12 text-center text-slate-400">
              <span className="material-symbols-outlined text-4xl block mb-2 text-slate-300">room_service</span>
              <p className="text-xs font-bold uppercase tracking-wider m-0">Không tìm thấy dịch vụ nào</p>
            </div>
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-100 bg-slate-50/50">
                  <th className="p-4 pl-6 text-[10px] font-black text-slate-400 uppercase tracking-widest">ID</th>
                  <th className="p-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">Tên dịch vụ</th>
                  <th className="p-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">Giá dịch vụ</th>
                  <th className="p-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">Đơn vị</th>
                  <th className="p-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">Trạng thái</th>
                  <th className="p-4 pr-6 text-[10px] font-black text-slate-400 uppercase tracking-widest text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 text-xs font-bold">
                {filteredServices.map((svc) => (
                  <tr key={svc.id} className="hover:bg-slate-50/50 transition-all">
                    <td className="p-4 pl-6 text-slate-400 font-mono">#{svc.id}</td>
                    <td className="p-4 text-slate-800">
                      <div>{svc.name}</div>
                      {svc.description && <div className="text-[10px] text-slate-400 font-normal mt-0.5">{svc.description}</div>}
                    </td>
                    <td className="p-4 text-primary font-black">{Number(svc.price).toLocaleString()} VNĐ</td>
                    <td className="p-4 text-slate-600">{svc.unit}</td>
                    <td className="p-4">
                      <span className={`px-2.5 py-1 rounded-full text-[9px] font-black uppercase tracking-wider ${
                        svc.isactive ? 'bg-emerald-50 text-emerald-600 border border-emerald-100' : 'bg-rose-50 text-rose-600 border border-rose-100'
                      }`}>
                        {svc.isactive ? 'Hoạt động' : 'Tạm ngưng'}
                      </span>
                    </td>
                    <td className="p-4 pr-6 text-right">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => handleOpenEdit(svc)}
                          className="bg-slate-50 border border-slate-200/60 text-slate-700 hover:bg-slate-100 text-[8.5px] font-black uppercase tracking-widest px-3 py-1.5 cursor-pointer rounded-lg transition-all"
                        >
                          Sửa
                        </button>
                        <button
                          onClick={() => handleDelete(svc.id, svc.name)}
                          className="bg-rose-50 border border-rose-100 text-rose-600 hover:bg-rose-100 text-[8.5px] font-black uppercase tracking-widest px-3 py-1.5 cursor-pointer rounded-lg transition-all"
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
      )}
    </div>
  );
};

export default ServicesManager;
