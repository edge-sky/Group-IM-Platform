import {useState, useEffect, useMemo, useRef} from 'react';
import {Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogClose} from '@/components/ui/dialog';
import {Button} from '@/components/ui/button';
import {Input} from '@/components/ui/input';
import {Search, Plus, UserX, Shield, Check, X} from 'lucide-react';
import api from '@/api/axiosConfig';
import { useToast } from '@/components/Toast/useToast';

const MemberList = ({open, currUid, conversationId, onOpenChange, chats}) => {
    const [members, setMembers] = useState([]);
    const [selectedMember, setSelectedMember] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [showInviteDialog, setShowInviteDialog] = useState(false);
    const [inviteId, setInviteId] = useState('');
    const [conversationCreator, setConversationCreator] = useState(null);

    const memberPermission = useRef(false); // 用户人员管理权限
    const isCreator = useRef(false); // 当前用户是否为创建者

    const { showToast, ToastContainer } = useToast();

    // 获取成员列表与权限
    useEffect(() => {
        if (!open || !conversationId) return;
        fetchData();
    }, [open]);

    const fetchData = async () => {
        if (conversationId) {
            // 从会话列表中获取创建者信息
            const currentChat = chats.find(chat => chat.id === conversationId);
            if (currentChat) {
                setConversationCreator(currentChat.managerUid);
                isCreator.current = (currentChat.managerUid === currUid);
            }

            // 获取权限
            await api.get(`/${conversationId}/conversation/permission`).then(response => {
                memberPermission.current = response.permission;
            });

            // 获取成员信息
            api.get(`/${conversationId}/conversation/userInfo`)
                .then(response => {
                    setMembers(response);
                    if (response.length > 0) {
                        setSelectedMember(response[0]); // 默认选中第一个
                    }
                })
                .catch(error => console.error('获取成员列表失败:', error));
        }
    }

    // 搜索过滤
    const filteredMembers = useMemo(() =>
        members.filter(member =>
            member.deleted !== 1 && member.username.toLowerCase().includes(searchTerm.toLowerCase())
        ), [members, searchTerm]);

    // 处理邀请
    const handleInvite = () => {
        if (!inviteId.trim()) return;
        api.post(`/${conversationId}/conversation/invite`, {
            invitedUid: inviteId.trim()
        }).then(() => {
            showToast('邀请已发送', 'success');
            setShowInviteDialog(false);
            setInviteId('');
        });
    };

    // 处理移除
    const handleRemove = (removeUid) => {
        if (!memberPermission.current) return;

        api.post(`/${conversationId}/conversation/removeUser`, {
            removeUid: removeUid
        }).then( () => {
            showToast('成员已移除', 'success');
            fetchData();
        })
    };

    // 处理权限更新
    // 处理权限更新
    const handlePermissionUpdate = (targetUid, permission, value) => {
        // 检查权限依赖关系
        if (permission === 'fileOperatePermission' && value === true) {
            // 如果要授予文件操作权限，必须先有文件查看权限
            if (!selectedMember?.fileVisiblePermission) {
                showToast('授予文件操作权限前，必须先授予文件查看权限', 'error');
                return;
            }
        }

        if (permission === 'fileVisiblePermission' && value === false) {
            // 如果要撤销文件查看权限，必须先撤销文件操作权限
            if (selectedMember?.fileOperatePermission) {
                showToast('撤销文件查看权限前，必须先撤销文件操作权限', 'error');
                return;
            }
        }

        api.post(`/${conversationId}/conversation/updatePermission`, {
            targetUid: targetUid,
            permission: permission,
            value: value
        }).then(() => {
            showToast('权限已更新', 'success');
            // 更新本地状态 - 修复变量名错误
            setMembers(prev => prev.map(member =>
                member.uid === targetUid
                    ? { ...member, [permission]: value }
                    : member
            ));
            if (selectedMember?.uid === targetUid) {
                setSelectedMember(prev => ({ ...prev, [permission]: value }));
            }
        }).catch(error => {
            showToast('权限更新失败', 'error');
            console.error('权限更新失败:', error);
        });
    };

    // 权限项组件
    // 权限项组件
    const PermissionItem = ({ title, permission, canEdit }) => {
        const hasPermission = selectedMember?.[permission] || false;

        // 检查是否应该禁用操作按钮
        const isDisabled = () => {
            if (!canEdit) return true;

            // 文件操作权限：如果没有文件查看权限，不能授予文件操作权限
            if (permission === 'fileOperatePermission' && !hasPermission && !selectedMember?.fileVisiblePermission) {
                return true;
            }

            // 文件查看权限：如果有文件操作权限，不能撤销文件查看权限
            if (permission === 'fileVisiblePermission' && hasPermission && selectedMember?.fileOperatePermission) {
                return true;
            }

            return false;
        };

        const getButtonText = () => {
            if (permission === 'fileOperatePermission' && !hasPermission && !selectedMember?.fileVisiblePermission) {
                return '需要文件查看权限';
            }
            if (permission === 'fileVisiblePermission' && hasPermission && selectedMember?.fileOperatePermission) {
                return '先撤销文件操作权限';
            }
            return hasPermission ? '撤销' : '授予';
        };

        return (
            <div className="flex items-center justify-between py-3 border-b border-gray-100 last:border-b-0">
                <div className="flex items-center">
                    <Shield className="h-4 w-4 text-gray-500 mr-2" />
                    <span className="text-sm font-medium">{title}</span>
                </div>
                <div className="flex items-center">
                    {hasPermission ? (
                        <Check className="h-4 w-4 text-green-500 mr-2" />
                    ) : (
                        <X className="h-4 w-4 text-red-500 mr-2" />
                    )}
                    <span className={`text-sm ${hasPermission ? 'text-green-600' : 'text-red-600'}`}>
                    {hasPermission ? '有权限' : '无权限'}
                </span>
                    {canEdit && (
                        <Button
                            variant="outline"
                            size="sm"
                            className="ml-3 h-7 px-2"
                            disabled={isDisabled()}
                            onClick={() => handlePermissionUpdate(selectedMember.uid, permission, !hasPermission)}
                        >
                            {getButtonText()}
                        </Button>
                    )}
                </div>
            </div>
        );
    };

    // 判断是否可以编辑权限
    const canEditPermissions = () => {
        if (!selectedMember) return false;
        // 如果选中的是创建者，不能编辑
        if (selectedMember.uid === conversationCreator) return false;
        // 如果当前用户是创建者，可以编辑所有权限
        if (isCreator.current) return true;
        // 如果当前用户有成员管理权限，只能编辑发言权限
        return memberPermission.current;
    };

    // 判断是否可以移除成员
    const canRemoveMember = () => {
        if (!selectedMember || !memberPermission.current) return false;
        // 不能移除自己
        if (selectedMember.uid === localStorage.getItem('uid')) return false;
        // 不能移除创建者
        if (selectedMember.uid === conversationCreator) return false;
        return true;
    };

    return (
        <>
            {/* Toast容器 */}
            <ToastContainer />
            {/* 主弹窗 */}
            <Dialog open={open} onOpenChange={onOpenChange}>
                <DialogContent className="max-w-4xl h-[70vh] flex flex-col p-0">
                    <DialogHeader className="p-6 pb-0">
                        <DialogTitle>人员管理</DialogTitle>
                    </DialogHeader>
                    <div className="flex-grow flex overflow-hidden">
                        {/* 左侧列表 */}
                        <div className="w-1/3 border-r flex flex-col">
                            <div className="p-4 border-b">
                                <div className="flex items-center space-x-2">
                                    <div className="relative flex-grow">
                                        <Search
                                            className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400"/>
                                        <Input
                                            placeholder="搜索成员..."
                                            className="pl-10"
                                            value={searchTerm}
                                            onChange={(e) => setSearchTerm(e.target.value)}
                                        />
                                    </div>
                                    {memberPermission.current && (
                                        <Button size="icon" variant="outline" onClick={() => setShowInviteDialog(true)}>
                                            <Plus className="h-4 w-4"/>
                                        </Button>
                                    )}
                                </div>
                            </div>
                            <div className="flex-grow overflow-y-auto">
                                {filteredMembers.map(member => (
                                    <div
                                        key={member.uid}
                                        className={`p-4 cursor-pointer hover:bg-gray-100 ${selectedMember?.uid === member.uid ? 'bg-blue-50' : ''}`}
                                        onClick={() => setSelectedMember(member)}
                                    >
                                        <div className="flex items-center">
                                            <img src={member.avatar || 'https://via.placeholder.com/150'}
                                                 alt={member.username} className="w-10 h-10 rounded-full mr-3"/>
                                            <div className="flex-1">
                                                <span className="font-medium">{member.username}</span>
                                                {member.uid === conversationCreator && (
                                                    <span className="ml-2 text-xs bg-yellow-100 text-yellow-800 px-2 py-1 rounded">
                                                        创建者
                                                    </span>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* 右侧详情 */}
                        <div className="w-2/3 p-6 flex flex-col">
                            {selectedMember ? (
                                <>
                                    <div className="flex-grow">
                                        {/* 用户信息 */}
                                        <div className="flex items-center mb-6">
                                            <img src={selectedMember.avatar || 'https://via.placeholder.com/150'}
                                                 alt={selectedMember.username} className="w-24 h-24 rounded-full mr-6"/>
                                            <div>
                                                <h2 className="text-2xl font-bold">{selectedMember.username}</h2>
                                                <p className="text-gray-500">UID: {selectedMember.uid}</p>
                                                {selectedMember.uid === conversationCreator && (
                                                    <span className="inline-block mt-2 text-sm bg-yellow-100 text-yellow-800 px-3 py-1 rounded-full">
                                                        会话创建者
                                                    </span>
                                                )}
                                            </div>
                                        </div>

                                        {/* 权限列表 */}
                                        <div className="bg-gray-50 rounded-lg p-4 mb-6">
                                            <h3 className="text-lg font-semibold mb-4">成员权限</h3>
                                            <div className="space-y-0">
                                                <PermissionItem
                                                    title="会话成员管理"
                                                    permission="memberPermission"
                                                    canEdit={isCreator.current && canEditPermissions()}
                                                />
                                                <PermissionItem
                                                    title="文件查看权限"
                                                    permission="fileVisiblePermission"
                                                    canEdit={isCreator.current && canEditPermissions()}
                                                />
                                                <PermissionItem
                                                    title="文件操作权限"
                                                    permission="fileOperatePermission"
                                                    canEdit={isCreator.current && canEditPermissions()}
                                                />
                                                <PermissionItem
                                                    title="发言权限"
                                                    permission="messagePermission"
                                                    canEdit={canEditPermissions()}
                                                />
                                            </div>
                                        </div>
                                    </div>

                                    {/* 操作按钮 */}
                                    {canRemoveMember() && (
                                        <div className="mt-auto">
                                            <Button variant="destructive" className="w-full"
                                                    onClick={() => handleRemove(selectedMember.uid)}>
                                                <UserX className="mr-2 h-4 w-4"/>
                                                移除成员
                                            </Button>
                                        </div>
                                    )}
                                </>
                            ) : (
                                <div className="flex items-center justify-center h-full text-gray-500">
                                    <p>从左侧选择一个成员以查看详情</p>
                                </div>
                            )}
                        </div>
                    </div>
                </DialogContent>
            </Dialog>

            {/* 邀请弹窗 */}
            <Dialog open={showInviteDialog} onOpenChange={setShowInviteDialog}>
                <DialogContent className="sm:max-w-md">
                    <DialogHeader>
                        <DialogTitle>邀请新成员</DialogTitle>
                    </DialogHeader>
                    <div className="py-4">
                        <Input
                            placeholder="输入用户ID"
                            value={inviteId}
                            onChange={(e) => setInviteId(e.target.value)}
                        />
                    </div>
                    <DialogFooter>
                        <DialogClose asChild>
                            <Button type="button" variant="secondary">取消</Button>
                        </DialogClose>
                        <Button type="button" onClick={handleInvite}>发送邀请</Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </>
    );
};

export default MemberList;
