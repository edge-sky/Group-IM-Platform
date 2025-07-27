import React from 'react';
import { LogOut, Settings, User } from 'lucide-react';

const Banner = ({ avatarUrl, username, userId, onLogout, onSettings }) => {
    return (
        <div className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between">
            {/* 左侧：应用标题 */}
            <div className="flex items-center">
                <h1 className="text-xl font-bold text-gray-800">协作平台</h1>
            </div>

            {/* 右侧：用户信息和操作 */}
            <div className="flex items-center space-x-4">
                {/* 用户信息 */}
                <div className="flex items-center space-x-3">
                    {/* 用户头像 */}
                    <img
                        src={avatarUrl || ''}
                        alt={username || '未知用户'}
                        className="w-10 h-10 rounded-full border-2 border-gray-200 object-cover"
                        onError={(e) => {
                            e.target.src = '';
                        }}
                    />

                    {/* 用户名和ID */}
                    <div className="flex flex-col">
                        <span className="text-sm font-medium text-gray-800">
                            {username || '未知用户'}
                        </span>
                        <span className="text-xs text-gray-500">
                            ID: {userId || 'N/A'}
                        </span>
                    </div>
                </div>

                {/* 操作按钮 */}
                <div className="flex items-center space-x-2">
                    {/* 设置按钮 */}
                    <button
                        onClick={onSettings}
                        className="p-2 rounded-lg hover:bg-gray-100 transition-colors"
                        title="设置"
                    >
                        <Settings className="h-5 w-5 text-gray-600" />
                    </button>

                    {/* 登出按钮 */}
                    <button
                        onClick={onLogout}
                        className="p-2 rounded-lg hover:bg-gray-100 transition-colors"
                        title="登出"
                    >
                        <LogOut className="h-5 w-5 text-gray-600" />
                    </button>
                </div>
            </div>
        </div>
    );
};

export default Banner;