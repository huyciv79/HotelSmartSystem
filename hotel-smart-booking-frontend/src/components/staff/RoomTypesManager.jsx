import { useState } from 'react';

const RoomTypesManager = ({
  roomTypes,
  isRoomFormOpen,
  setIsRoomFormOpen,
  editingRoom,
  roomFormData,
  setRoomFormData,
  handleFileChange,
  handleRoomSubmit,
  handleOpenAddRoom,
  handleOpenEditRoom,
  handleDeleteRoom,
  isSubmittingRoom
}) => {
  return (
    <div className="space-y-6 animate-scale-in text-left">
      {/* Header */}
      <div className="flex justify-between items-end">
        <div>
          <h3 className="text-slate-800 font-black text-base uppercase tracking-wider m-0">Quản lý loại phòng</h3>
          <p className="text-[10px] text-slate-400 font-bold uppercase tracking-widest mt-1">Cấu hình thông tin danh mục phòng ngủ và đơn giá</p>
        </div>

        <button
          onClick={handleOpenAddRoom}
          className="bg-primary hover:brightness-110 text-white font-black text-xs uppercase tracking-widest px-5 py-3 border-none cursor-pointer transition-all flex items-center gap-1.5 rounded-xl shadow-[0_4px_12px_rgba(162,5,19,0.2)]"
        >
          <span className="material-symbols-outlined text-sm">add</span>
          Thêm loại phòng
        </button>
      </div>

      {/* Room Form */}
      {isRoomFormOpen && (
        <div className="bg-white border border-slate-100 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.015)] p-6 md:p-8">
          <h4 className="text-xs font-black text-slate-800 uppercase tracking-widest border-b border-slate-100 pb-3 mb-6">
            {editingRoom ? `Chỉnh sửa: ${editingRoom.name}` : 'Thêm mới loại phòng'}
          </h4>

          <form onSubmit={handleRoomSubmit} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Tên loại phòng</label>
                <input
                  type="text"
                  value={roomFormData.name}
                  onChange={(e) => setRoomFormData(prev => ({ ...prev, name: e.target.value }))}
                  placeholder="E.g., Suite River View"
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all placeholder:text-slate-400"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Đơn giá cơ bản (VND / đêm)</label>
                <input
                  type="number"
                  value={roomFormData.basePrice}
                  onChange={(e) => setRoomFormData(prev => ({ ...prev, basePrice: e.target.value }))}
                  placeholder="E.g., 5000000"
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all placeholder:text-slate-400"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Sức chứa người lớn / phòng</label>
                <input
                  type="number"
                  value={roomFormData.adultCapacity}
                  onChange={(e) => setRoomFormData(prev => ({ ...prev, adultCapacity: e.target.value }))}
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Sức chứa trẻ em / phòng</label>
                <input
                  type="number"
                  value={roomFormData.childCapacity}
                  onChange={(e) => setRoomFormData(prev => ({ ...prev, childCapacity: e.target.value }))}
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Loại giường</label>
                <input
                  type="text"
                  value={roomFormData.bedType}
                  onChange={(e) => setRoomFormData(prev => ({ ...prev, bedType: e.target.value }))}
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Diện tích phòng (m²)</label>
                <input
                  type="number"
                  value={roomFormData.roomSize}
                  onChange={(e) => setRoomFormData(prev => ({ ...prev, roomSize: e.target.value }))}
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all"
                />
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Trạng thái hoạt động</label>
                <select
                  value={roomFormData.status}
                  onChange={(e) => setRoomFormData(prev => ({ ...prev, status: e.target.value }))}
                  className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-2.5 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all [&>option]:bg-white [&>option]:text-slate-800"
                >
                  <option value="Active">Active</option>
                  <option value="Inactive">Inactive</option>
                </select>
              </div>

              <div className="space-y-2">
                <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Hình ảnh phòng</label>
                <input
                  type="file"
                  onChange={handleFileChange}
                  className="w-full text-xs text-slate-500 file:mr-4 file:py-2 file:px-4 file:border file:border-slate-200/60 file:rounded-lg file:text-[10px] file:font-black file:uppercase file:bg-slate-50 file:text-slate-700 hover:file:bg-slate-100 cursor-pointer"
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="block text-[10px] font-bold text-slate-400 uppercase tracking-widest">Mô tả loại phòng</label>
              <textarea
                rows="3"
                value={roomFormData.description}
                onChange={(e) => setRoomFormData(prev => ({ ...prev, description: e.target.value }))}
                className="w-full bg-slate-50 border border-slate-200/60 rounded-xl px-4 py-3 font-bold text-xs outline-none text-slate-800 focus:border-primary focus:bg-white transition-all resize-none"
              />
            </div>

            <div className="flex gap-3">
              <button
                type="submit"
                disabled={isSubmittingRoom}
                className="bg-primary hover:brightness-110 text-white font-bold px-8 py-3.5 uppercase text-xs tracking-widest transition-all cursor-pointer border-none flex items-center justify-center gap-1.5 rounded-xl shadow-[0_4px_12px_rgba(162,5,19,0.2)] disabled:opacity-60"
              >
                {isSubmittingRoom ? 'Đang lưu...' : 'Lưu lại'}
              </button>
              <button
                type="button"
                onClick={() => setIsRoomFormOpen(false)}
                className="bg-slate-50 hover:bg-slate-100 text-slate-700 font-bold px-8 py-3.5 uppercase text-xs tracking-widest border border-slate-200/60 cursor-pointer rounded-xl transition-all"
              >
                Hủy bỏ
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Room Types Table */}
      <div className="bg-white border border-slate-100 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.015)] overflow-hidden">
        <table className="w-full border-collapse text-left text-xs text-slate-600">
          <thead>
            <tr className="border-b border-slate-100 text-[9px] font-black uppercase tracking-wider text-slate-400 bg-slate-50/50">
              <th className="p-4">Hình ảnh</th>
              <th className="p-4">Tên loại phòng</th>
              <th className="p-4">Giá / đêm</th>
              <th className="p-4">Giường / Diện tích</th>
              <th className="p-4">Sức chứa tối đa</th>
              <th className="p-4">Trạng thái</th>
              <th className="p-4 text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {roomTypes.map((room) => (
              <tr key={room.id} className="hover:bg-slate-50/50 transition-colors">
                <td className="p-4">
                  <img
                    src={room.primaryImageUrl || 'https://images.unsplash.com/photo-1590490360182-c33d57733427?w=100&q=80'}
                    alt={room.name}
                    className="w-16 h-12 object-cover rounded-lg border border-slate-100"
                  />
                </td>
                <td className="p-4">
                  <span className="text-slate-800 font-black uppercase text-sm block">{room.name}</span>
                  <span className="text-[9px] text-slate-400 font-bold uppercase tracking-wider block mt-0.5">ID: {room.id}</span>
                </td>
                <td className="p-4 text-primary font-black">
                  {new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(room.basePrice || room.baseprice || 0)}
                </td>
                <td className="p-4 font-semibold text-slate-500">
                  {room.bedType || 'King Bed'} <br />
                  <span className="text-slate-400">{room.area || room.roomSize || room.roomsize || 35} m²</span>
                </td>
                <td className="p-4 font-semibold text-slate-500">
                  {room.adultCapacity || room.adultcapacity || 2} NL • {room.childCapacity || room.childcapacity || 1} TE
                </td>
                <td className="p-4">
                  <span className={`px-2.5 py-1 text-[8.5px] font-extrabold uppercase tracking-widest rounded-lg ${
                    room.status === 'Active' || !room.status
                      ? 'bg-emerald-50 text-emerald-600 border border-emerald-100'
                      : 'bg-rose-50 text-rose-600 border border-rose-100'
                  }`}>
                    {room.status || 'Active'}
                  </span>
                </td>
                <td className="p-4 text-right">
                  <div className="flex justify-end gap-2">
                    <button
                      onClick={() => handleOpenEditRoom(room)}
                      className="bg-slate-50 border border-slate-200/60 text-slate-700 hover:bg-slate-100 hover:text-slate-900 text-[8.5px] font-black uppercase tracking-widest px-3 py-1.5 cursor-pointer rounded-lg transition-all"
                    >
                      Sửa
                    </button>
                    <button
                      onClick={() => handleDeleteRoom(room.id, room.name)}
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
      </div>
    </div>
  );
};

export default RoomTypesManager;
