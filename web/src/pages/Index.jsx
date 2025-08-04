import {useEffect, useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {File as FileIcon, Files, Folder as FolderIcon, Mailbox, MessageSquare, Plus, Search, Users} from 'lucide-react';
import {ResizableHandle, ResizablePanel, ResizablePanelGroup} from '@/components/ui/resizable';
import {Dialog, DialogContent, DialogHeader, DialogTitle} from '@/components/ui/dialog';
import api from "@/api/axiosConfig";
import FileTree from '@/components/FileCompont/FileTree';
import {DndProvider} from 'react-dnd';
import {HTML5Backend} from 'react-dnd-html5-backend';
import MemberList from "@/components/MemberCompont/MemberList.jsx";
import Banner from "@/components/Banner.jsx";
import {useToast} from '@/components/Toast/useToast';

const Index = () => {
    const [activeChat, setActiveChat] = useState(null);
    const [inputMessage, setInputMessage] = useState('');
    const [showFiles, setShowFiles] = useState(false);
    const [showMailbox, setShowMailbox] = useState(false);
    const [chats, setChats] = useState([]);
    const [loading, setLoading] = useState(true);
    const [mailboxMessages, setMailboxMessages] = useState([]);
    const navigate = useNavigate();
    const messagesEndRef = useRef(null);
    const [shouldScroll, setShouldScroll] = useState(true);
    const [userInfo, setUserInfo] = useState(new Map());
    const [currUserInfo, setCurrUserInfo] = useState(null);
    const [showVersionBox, setShowVersionBox] = useState(false);
    const [showMemberBox, setShowMemberBox] = useState(false);
    const [showNewConversationDialog, setShowNewConversationDialog] = useState(false);
    const [newConversationName, setNewConversationName] = useState('');

    // 加载历史消息
    const [isLoadingHistory, setIsLoadingHistory] = useState(false);
    const [hasMoreMessages, setHasMoreMessages] = useState(true);
    const messagesContainerRef = useRef(null);
    const previousScrollHeight = useRef(0);

    // 连接状态
    const [connectionStatus, setConnectionStatus] = useState('connecting'); // 'connecting', 'connected', 'disconnected'
    const [showConnectionToast, setShowConnectionToast] = useState(false);
    const [isReconnecting, setIsReconnecting] = useState(false);

    // toast
    const {showToast, ToastContainer} = useToast();

    const uid = localStorage.getItem('uid');
    const token = localStorage.getItem('token');

    // useRef相当于一个对象，生命周期贯穿整个组件的生命周期
    const shouldClose = useRef(true);
    const socketRef = useRef(null);
    const activeChatRef = useRef(activeChat);
    const filePanelRef = useRef(null);
    const newMessageRef = useRef(false);

    const [messages, setMessages] = useState([]);
    const messageRef = useRef([]);

    // 跨文件状态同步
    const [onMessage, setOnMessage] = useState(null);

    // navigate为空，只会在组件挂载时执行一次
    useEffect(() => {
        verifyToken().then(isValid => {
            // 拉取当前用户
            fetchCurrUserInfo();

            // 获取会话列表
            fetchConversationsList();
        });
    }, [navigate]);

    // 当message发生变化且允许滚动时才滚动至底部
    useEffect(() => {
        if (shouldScroll) {
            scrollToBottom(false);
            setShouldScroll(false);
        }
    }, [messageRef.current, shouldScroll]);

    // socketRef在渲染循环之外，
    useEffect(() => {
        activeChatRef.current = activeChat;
    }, [activeChat]);

    // 文件版本列表显示
    useEffect(() => {
        // 文件版本列表存在时才允许关闭文件列表
        if (showVersionBox) {
            // 如果文件版本列表显示，则不允许关闭文件面板
            shouldClose.current = false;
        } else if (!shouldClose.current) {
            // 如果文件版本列表隐藏，则允许关闭文件面板
            shouldClose.current = true;
        }
    }, [showVersionBox]);

    // 文件列表显示
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (!shouldClose.current) {
                return;
            }

            // 当文件面板显示时，监听外部点击事件
            if (filePanelRef.current
                && !filePanelRef.current.contains(event.target)) {
                setShowFiles(false);
            }
        };

        if (showFiles) {
            document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [showFiles]);

    // 监听文件或文件夹变更消息
    useEffect(() => {
        if (!onMessage) return;

        let content = '';
        let type = onMessage.type;
        if (type === 0) {
            // 发送普通信息
            content = onMessage.messages;
            socketRef.current.send(JSON.stringify({
                "request": "sendMessage",
                "conversationId": activeChatRef.current,
                "content": content,
                "type": type
            }));
        } else if (type === 1) {
            // 文件夹创建
            content = onMessage.folderName;
            socketRef.current.send(JSON.stringify({
                "request": "sendMessage",
                "conversationId": activeChatRef.current,
                "content": content,
                "type": type
            }));
        } else if (type === 2) {
            // 文件上传
            for (let i = 0; i < onMessage.files.length; i++) {
                content += onMessage.files[i].name + (i < onMessage.files.length - 1 ? ', ' : '');
            }
            socketRef.current.send(JSON.stringify({
                "request": "sendMessage",
                "conversationId": activeChatRef.current,
                "content": content,
                "type": type
            }));
        } else if (type === 3 || type === 4) {
            // 文件移动
            content = `${onMessage.source}:::${onMessage.target}`;
            socketRef.current.send(JSON.stringify({
                "request": "sendMessage",
                "conversationId": activeChatRef.current,
                "content": content,
                "type": type
            }));
        } else if (type === 5) {
            // 文件删除
            content = onMessage.folderName;
            socketRef.current.send(JSON.stringify({
                "request": "sendMessage",
                "conversationId": activeChatRef.current,
                "content": content,
                "type": type
            }));
        } else if (type === 6) {
            // 文件夹删除
            content = onMessage.folderName;
            socketRef.current.send(JSON.stringify({
                "request": "sendMessage",
                "conversationId": activeChatRef.current,
                "content": content,
                "type": type
            }));
        } else {
            return;
        }

        messageRef.current = [...messageRef.current, {
            id: (messageRef.current.at(-1)?.id || 0) + 1,
            text: content,
            fromUid: localStorage.getItem('uid'),
            type: type,
            time: new Date().toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})
        }];
        setMessages(messageRef.current);
        setShouldScroll(true); // 滚动到最新消息
    }, [onMessage]);

    // 当信箱显示时，拉取消息
    useEffect(() => {
        if (showMailbox) {
            fetchMailboxMessages();
        }
    }, [showMailbox]);

    // 监听滚动事件，检测是否到达顶部
    useEffect(() => {
        const container = messagesContainerRef.current;
        if (!container) return;

        const handleScroll = () => {
            const {scrollTop, scrollHeight, clientHeight} = container;

            // 当滚动到顶部附近（距离顶部小于50px）且有更多消息时，加载历史消息
            if (scrollTop < 50 && !isLoadingHistory && hasMoreMessages && messageRef.current.length > 0) {
                loadHistoryMessages();
            }
        };

        container.addEventListener('scroll', handleScroll);
        return () => container.removeEventListener('scroll', handleScroll);
    }, [isLoadingHistory, hasMoreMessages, messageRef.current.length]);

    // 加载历史消息
    const loadHistoryMessages = async () => {
        if (!activeChat || messageRef.current.length === 0 || isLoadingHistory) return;

        setIsLoadingHistory(true);

        try {
            // 获取最早的消息ID
            const earliestMessageId = Math.min(...messageRef.current.map(msg => msg.id));

            // 发送WebSocket请求获取历史消息
            socketRef.current.send(JSON.stringify({
                "request": "fetchMessages",
                "conversationId": activeChat,
                "earliestMessageId": earliestMessageId,
                "limit": 20
            }));
        } catch (error) {
            console.error('加载历史消息失败:', error);
            setIsLoadingHistory(false);
        }
    };

    const sleep = (time) => {
        return new Promise((resolve) => setTimeout(resolve, time));
    }

    const scrollToBottom = (smooth) => {
        messagesEndRef.current?.scrollIntoView({behavior: smooth ? 'smooth' : 'instant'});
    };


    const connectWebSocket = () => {
        if (socketRef.current) {
            socketRef.current.close();
        }

        setConnectionStatus('connecting');
        socketRef.current = new WebSocket(`ws://localhost:8080/ws?Authorization=${token}`);

        socketRef.current.onopen = () => {
            console.log('WebSocket connection established');
            setConnectionStatus('connected');
            setShowConnectionToast(false);
            setIsReconnecting(false);

            // 重新连接后刷新当前会话消息
            if (activeChatRef.current) {
                messageRef.current = [];
                setMessages([]);
                setHasMoreMessages(true);
                socketRef.current.send(JSON.stringify({
                    "request": "fetchMessages",
                    "conversationId": activeChatRef.current,
                    "earliestMessageId": 0,
                    "limit": 50
                }));
            }
        };

        socketRef.current.onmessage = (event) => {
            const data = JSON.parse(event.data);
            console.log('Message received from server:', data);
            const respond = data.respond;
            if (respond === 'unreadNotification') {
                // 未读通知

            } else if (respond === 'messagesList') {
                // 当前会话的消息
                if (data.code === "200" && data.payload) {
                    const formattedMessages = data.payload.reverse().map(msg => ({
                        id: msg.messageId,
                        text: msg.content,
                        fromUid: msg.fromUid,
                        type: msg.type || 0,
                        time: new Date(msg.time).toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})
                    }));

                    if (messageRef.current.length > 0) {
                        if (data.payload.length === 0) {
                            setHasMoreMessages(false);
                            setIsLoadingHistory(false);
                            return
                        }

                        const newMessages = [...formattedMessages, ...messageRef.current];
                        messageRef.current = newMessages;
                        setMessages(newMessages);
                        setIsLoadingHistory(false);
                        setHasMoreMessages(true);

                    } else {
                        messageRef.current = formattedMessages;
                        setMessages(formattedMessages);
                        setShouldScroll(true);
                    }
                } else {
                    messageRef.current = [];
                }
            } else if (respond === 'receiveMessage') {
                fetchConversationsList();
                if (data.conversationId === activeChatRef.current) {
                    const newMessage = {
                        id: data.messageId,
                        text: data.content,
                        fromUid: data.fromUid,
                        type: data.type || 0,
                        time: new Date(data.time).toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'})
                    };

                    if (data.fromUid !== localStorage.getItem('uid')) {
                        messageRef.current = [...messageRef.current, newMessage];
                        setMessages(messageRef.current);
                        setShouldScroll(true);
                    }
                }
            }
        };

        socketRef.current.onerror = (error) => {
            console.error('WebSocket error:', error);
            setConnectionStatus('disconnected');
            setShowConnectionToast(true);
        };

        socketRef.current.onclose = (event) => {
            console.log('WebSocket connection closed:', event.reason);
            setConnectionStatus('disconnected');
            setShowConnectionToast(true);
        };
    };

    // 重新连接函数
    const handleReconnect = () => {
        setIsReconnecting(true);
        setTimeout(() => {
            setIsReconnecting(false);
            setShowConnectionToast(false);
            connectWebSocket();
        }, 5000);
        connectWebSocket();
    };

    // 验证用户登录状态
    const verifyToken = async () => {
        if (!token) {
            showToast('登录已过期', 'error');
            navigate('/auth');
            return false;
        }

        try {
            await api.post('/user/verify').then(response => {
                if (!response) {
                    throw new Error('Token验证失败');
                }
            });

            connectWebSocket();
        } catch (error) {
            localStorage.removeItem('token');
            localStorage.removeItem('uid');
            navigate('/auth');
            return false;
        }
    };

    const fetchCurrUserInfo = async () => {
        api.get('/user/info').then(response => {
            if (response && response.code === "200") {
                const user = response.user;
                setCurrUserInfo({
                    uid: user.uid,
                    username: user.username,
                    avatar: user.avatar || ''
                })
            } else {
                console.error('获取用户信息失败:', response);
            }
        }).catch(error => {
            console.error('获取用户信息失败:', error);
        });
    }

    // 获取会话列表
    const fetchConversationsList = async () => {
        try {
            setLoading(true);
            // 拉取会话列表
            await api.get('/0/conversation/list').then(response => {
                // 转换后端返回的会话数据格式以匹配前端需求
                const formattedChats = response.map(conv => ({
                    id: conv.id,
                    name: conv.name,
                    lastMessageContent: conv.lastMessageContent || '',
                    lastMessageTime: conv.lastMessageTime || '',
                    userNum: conv.userNum,
                    managerUid: conv.managerUid
                }));
                setChats(formattedChats);
            });
        } catch (error) {
            console.error('获取会话列表失败:', error);
        } finally {
            setLoading(false);
        }
    };

    // 拉取会话消息
    const fetchConversationMessages = async (conversationId, earliestMessageId) => {
        setActiveChat(conversationId);
        messageRef.current = [];
        setHasMoreMessages(true); // 重置状态
        setIsLoadingHistory(false);

        socketRef.current.send(JSON.stringify({
            "request": "fetchMessages",
            "conversationId": conversationId,
            "earliestMessageId": earliestMessageId,
            "limit": 50
        }));

        // 拉取会话用户信息
        api.get(`${conversationId}/conversation/userInfo`).then(response => {
            const userMap = new Map();
            response.forEach(user => {
                userMap.set(user.uid, {
                    username: user.username,
                    avatar: user.avatar || ''
                });
            })
            setUserInfo(userMap);
        });
    };

    // 发送消息
    const handleSendMessage = () => {
        if (inputMessage.trim()) {
            setOnMessage({
                type: 0,
                messages: inputMessage
            });

            setInputMessage(''); // 清空输入框
        }
    };

    // 获取信箱消息
    const fetchMailboxMessages = async () => {
        try {
            const response = await api.get('/user/inviteList');
            if (response && response.code === "200" && Array.isArray(response.invites)) {
                setMailboxMessages(response.invites);
            } else {
                setMailboxMessages([]);
            }
        } catch (error) {
            console.error('获取信箱消息失败:', error);
            setMailboxMessages([]);
        }
    };

    // 处理邀请响应
    const handleInvitationResponse = async (inviteInfo, accept) => {
        const updatedInviteInfo = {
            ...inviteInfo,
            accept: accept,
        }
        try {
            await api.post('/user/join', {
                inviteInfo: updatedInviteInfo
            }).then(() => {
                showToast('已加入该会话', "info");
                // 响应后刷新信箱
                fetchMailboxMessages();
                if (accept) {
                    // 如果接受了邀请，刷新会话列表
                    fetchConversationsList();
                }
            })
        } catch (error) {
            console.error('处理邀请失败:', error);
        }
    };

    // 新建会话
    const handleNewConversation = () => {
        setShowNewConversationDialog(true);
    };

    // 处理创建会话逻辑
    const handleCreateConversation = async () => {
        if (!newConversationName.trim()) {
            alert("会话名称不能为空");
            return;
        }
        try {
            await api.post('/0/conversation/create', {
                name: newConversationName
            });
            setShowNewConversationDialog(false);
            setNewConversationName('');
            await fetchConversationsList(); // 刷新会话列表
        } catch (error) {
            console.error("创建会话失败:", error);
            alert("创建会话失败");
        }
    };


    // 渲染文件面板
    const renderFilePanel = () => (
        <div
            ref={filePanelRef}
            className="absolute inset-y-0 right-0 w-2/5 bg-white border-l shadow-lg transition-all duration-300 ease-in-out z-10">
            <div className="h-full flex flex-col">
                <div className="flex-1 overflow-y-auto p-4">
                    <FileTree
                        activeChat={activeChat}
                        onModalStateChange={setShowVersionBox}
                        onFileMessageChange={setOnMessage}
                    />
                </div>
            </div>
        </div>
    );

    const renderMemberPanel = () => (
        <MemberList
            open={showMemberBox}
            currUid={uid}
            onOpenChange={setShowMemberBox}
            conversationId={activeChat}
            chats={chats}
        />
    );

    // 渲染消息内容
    const renderMessageContent = (msg) => {
        const isSender = msg.fromUid === uid;
        const bubbleColor = isSender ? 'bg-blue-500 text-white' : 'bg-gray-200';
        const textColor = isSender ? 'text-white' : 'text-gray-800';

        if (msg.type >= 1 && msg.type <= 4) { // 文件夹创建/文件上传/移动
            const isMove = msg.type === 3 || msg.type === 4;
            const isFolder = msg.type === 1 || msg.type === 4;
            const Icon = isFolder ? FolderIcon : FileIcon;
            const title = isMove
                ? (isFolder ? "文件夹移动" : "文件移动")
                : (isFolder ? "文件夹创建" : "文件上传");

            let content;
            if (isMove) {
                const [source, target] = msg.text.split(':::');
                content = (
                    <div className={`space-y-1 ${textColor}`}>
                        <p><span className="font-semibold">从:</span> {source}</p>
                        <p><span className="font-semibold">到:</span> {target}</p>
                    </div>
                );
            } else {
                content = <p className={textColor}>{msg.text}</p>;
            }

            return (
                <div
                    className={`break-words max-w-sm md:max-w-lg rounded-lg p-4 ${bubbleColor} flex flex-col`}>
                    <div className="flex items-center mb-2">
                        <Icon className={`h-6 w-6 mr-3 ${textColor}`}/>
                        <p className={`font-bold text-lg ${textColor}`}>{title}</p>
                    </div>
                    <div className={`border-t ${isSender ? 'border-white/50' : 'border-gray-300'} pt-2`}>
                        {content}
                    </div>
                </div>
            );
        }

        // Default text message
        return (
            <div
                className={`break-words max-w-xs md:max-w-md rounded-lg p-3 ${bubbleColor}`}>
                <p>{msg.text}</p>
            </div>
        );
    };

    // 渲染连接状态提示组件
    const renderConnectionToast = () => {
        if (!showConnectionToast || connectionStatus === 'connected') return null;

        return (
            <div className="fixed top-4 left-1/2 transform -translate-x-1/2 z-50">
                <div className="bg-red-500 text-white px-4 py-3 rounded-lg shadow-lg flex items-center space-x-3">
                    <div className="flex items-center space-x-2">
                        <div className="w-3 h-3 bg-white rounded-full animate-pulse"></div>
                        <span>连接已断开</span>
                    </div>
                    <button
                        onClick={handleReconnect}
                        disabled={isReconnecting}
                        className="bg-white text-red-500 px-3 py-1 rounded text-sm font-medium hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed flex items-center space-x-1"
                    >
                        {isReconnecting && (
                            <div
                                className="w-3 h-3 border border-red-500 border-t-transparent rounded-full animate-spin"></div>
                        )}
                        <span>{isReconnecting ? '重连中...' : '重新连接'}</span>
                    </button>
                </div>
            </div>
        );
    };

    return (
        <DndProvider backend={HTML5Backend}>
            {/* Toast容器 */}
            <ToastContainer/>

            {/* 连接状态提示 */}
            {renderConnectionToast()}

            <div className="h-screen bg-gray-50 relative flex flex-col">
                <Banner
                    avatarUrl={currUserInfo?.avatar || ''}
                    username={currUserInfo?.username || '未知用户'}
                    userId={uid}
                    onLogout={() => {
                        showToast("登出成功", "success");

                        localStorage.removeItem('token');
                        localStorage.removeItem('uid');
                        navigate('/auth');
                    }}
                    onSettings={() => {
                        // 处理设置逻辑
                        showToast("这是设置", "info")
                    }}
                />
                <ResizablePanelGroup direction="horizontal" className="h-full">
                    {/* 左侧会话列表 */}
                    <ResizablePanel defaultSize={25} minSize={20} className="bg-white">
                        <div className="h-full flex flex-col">
                            <div className="p-4">
                                <h2 className="text-xl font-semibold">会话列表</h2>
                            </div>

                            <div className="p-2 flex items-center space-x-2">
                                <div className="relative flex-grow">
                                    <Search
                                        className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400"/>
                                    <input
                                        type="text"
                                        placeholder="搜索会话..."
                                        className="w-full pl-10 pr-4 py-2 rounded-lg bg-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>
                                <button
                                    onClick={handleNewConversation}
                                    className="p-2 rounded-lg bg-gray-100 hover:bg-gray-200 flex-shrink-0"
                                    title="新建会话"
                                >
                                    <Plus className="h-5 w-5 text-gray-600"/>
                                </button>
                            </div>

                            <div className="flex-1 overflow-y-auto">
                                {loading ? (
                                    <div className="p-4 text-center text-gray-500">加载中...</div>
                                ) : chats.length > 0 ? (
                                    chats.map(chat => (
                                        <div
                                            key={chat.id}
                                            className={`p-4 cursor-pointer hover:bg-gray-50 ${activeChat === chat.id ? 'bg-blue-50' : ''}`}
                                            onClick={() => fetchConversationMessages(chat.id, 0)}
                                        >
                                            <div className="flex justify-between items-start">
                                                <h3 className="font-medium">{chat.name}</h3>
                                                {chat.unread > 0 && (
                                                    <span
                                                        className="bg-blue-500 text-white text-xs px-2 py-1 rounded-full">
                                                      {chat.unread}
                                                    </span>
                                                )}
                                            </div>
                                            <p className="text-sm text-gray-500 truncate">{chat.lastMessageContent}</p>
                                        </div>
                                    ))
                                ) : (
                                    <div className="p-4 text-center text-gray-500">暂无会话</div>
                                )}
                            </div>

                            {/* 底部信箱按钮 */}
                            <div className="p-4 flex justify-center">
                                <button
                                    className="w-full py-3 rounded-lg bg-gray-100 hover:bg-gray-200 flex items-center justify-center"
                                    onClick={() => setShowMailbox(true)}
                                >
                                    <Mailbox className="h-5 w-5 text-gray-600 mr-2"/>
                                    <span>信箱</span>
                                </button>
                            </div>
                        </div>
                    </ResizablePanel>

                    <ResizableHandle withHandle/>

                    {/* 右侧会话窗口 */}
                    <ResizablePanel defaultSize={75} className="bg-white">
                        {activeChat ? (
                            <div className="h-full flex flex-col">
                                <div className="p-4 flex items-center justify-between">
                                    <h2 className="text-xl font-semibold">
                                        {chats.find(c => c.id === activeChat)?.name}
                                    </h2>
                                    <div>
                                        <button
                                            onClick={() => {
                                                setShowMemberBox(!showMemberBox);
                                            }}
                                            className="p-2 rounded hover:bg-gray-100"
                                            title="成员管理"
                                        >
                                            <Users className="h-5 w-5"/>
                                        </button>
                                        <button
                                            onClick={() => {
                                                setShowFiles(!showFiles);
                                            }}
                                            className="p-2 rounded hover:bg-gray-100"
                                            title="文件管理"
                                        >
                                            <Files className="h-5 w-5"/>
                                        </button>
                                    </div>
                                </div>

                                <div
                                    ref={messagesContainerRef}
                                    className="flex-1 overflow-y-auto p-4 space-y-4"
                                >
                                    {/* 加载历史消息的指示器 */}
                                    {isLoadingHistory && (
                                        <div className="flex justify-center py-2">
                                            <div className="flex items-center text-gray-500 text-sm">
                                                <div
                                                    className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-500 mr-2"></div>
                                                加载历史消息...
                                            </div>
                                        </div>
                                    )}

                                    {/* 没有更多历史消息的提示 */}
                                    {!hasMoreMessages && messageRef.current.length > 0 && (
                                        <div className="flex justify-center py-2">
                                            <span className="text-gray-400 text-sm">已显示全部消息</span>
                                        </div>
                                    )}

                                    {messages.length > 0 ? (
                                        messages.map((msg, index) => (
                                            <div key={msg.id || index}
                                                 className={`flex ${msg.fromUid === uid ? 'justify-end' : 'justify-start'}`}>
                                                {msg.fromUid !== uid && (
                                                    <img
                                                        src={userInfo.get(msg.fromUid)?.avatar}
                                                        alt={userInfo.get(msg.fromUid)?.username}
                                                        className="w-8 h-8 rounded-full mr-2"
                                                    />
                                                )}

                                                <div className="flex flex-col">
                                                    {msg.fromUid !== uid && (
                                                        <p className="text-xs text-gray-500 mb-1">
                                                            {userInfo.get(msg.fromUid)?.username || '未知用户'}
                                                        </p>
                                                    )}
                                                    {renderMessageContent(msg)}
                                                </div>

                                                {msg.fromUid === uid && (
                                                    <img
                                                        src={userInfo.get(msg.fromUid)?.avatar}
                                                        alt={userInfo.get(msg.fromUid)?.username}
                                                        className="w-8 h-8 rounded-full ml-2"
                                                    />
                                                )}
                                            </div>
                                        ))
                                    ) : (
                                        <div className="h-full flex items-center justify-center text-gray-400">
                                            <p>开始新的对话</p>
                                        </div>
                                    )}
                                    <div ref={messagesEndRef}/>
                                </div>

                                <div className="p-4">
                                    <div className="flex items-center">
                                        <input
                                            type="text"
                                            value={inputMessage}
                                            onChange={(e) => setInputMessage(e.target.value)}
                                            placeholder="输入消息..."
                                            className="flex-1 border rounded-l-lg px-4 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                                            onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
                                        />
                                        <button
                                            onClick={handleSendMessage}
                                            className="bg-blue-500 text-white px-4 py-2 rounded-r-lg hover:bg-blue-600"
                                        >
                                            发送
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ) : (
                            <div className="h-full flex items-center justify-center text-gray-400">
                                <div className="text-center">
                                    <MessageSquare className="h-12 w-12 mx-auto mb-4"/>
                                    <p>请从左侧选择或创建一个会话</p>
                                </div>
                            </div>
                        )}
                    </ResizablePanel>
                </ResizablePanelGroup>

                {/* 文件面板 - 从右侧滑出 */}
                {showFiles && renderFilePanel()}

                {/* 人员面板 */}
                {showMemberBox && renderMemberPanel()}

                {/* 新建会话弹窗 */}
                <Dialog open={showNewConversationDialog} onOpenChange={setShowNewConversationDialog}>
                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>新建会话</DialogTitle>
                        </DialogHeader>
                        <div className="mt-4">
                            <input
                                type="text"
                                placeholder="输入会话名称"
                                value={newConversationName}
                                onChange={(e) => setNewConversationName(e.target.value)}
                                className="w-full p-2 border rounded"
                                onKeyPress={(e) => e.key === 'Enter' && handleCreateConversation()}
                            />
                        </div>
                        <div className="mt-4 flex justify-end space-x-2">
                            <button
                                onClick={() => setShowNewConversationDialog(false)}
                                className="px-4 py-2 rounded-lg bg-gray-200 hover:bg-gray-300"
                            >
                                取消
                            </button>
                            <button
                                onClick={handleCreateConversation}
                                className="px-4 py-2 rounded-lg bg-blue-500 text-white hover:bg-blue-600"
                            >
                                创建
                            </button>
                        </div>
                    </DialogContent>
                </Dialog>

                {/* 信箱弹窗 */}
                <Dialog open={showMailbox} onOpenChange={setShowMailbox}>
                    <DialogContent className="sm:max-w-[600px] max-h-[80vh] overflow-y-auto">
                        <DialogHeader>
                            <DialogTitle className="text-xl">信箱</DialogTitle>
                        </DialogHeader>
                        <div className="space-y-4 mt-4">
                            {mailboxMessages.length > 0 ? (
                                mailboxMessages.map((invite) => (
                                    <div
                                        key={invite.token}
                                        className="p-4 rounded-lg border bg-blue-50 border-blue-200"
                                    >
                                        <div className="flex justify-between items-start">
                                            <h3 className="font-medium">会话邀请</h3>
                                            <span
                                                className="text-sm text-gray-500">{new Date(invite.timestamp).toLocaleString()}</span>
                                        </div>
                                        <p className="mt-2 text-gray-600">
                                            您被邀请加入一个新的会话 ID: {invite.conversationId}。
                                        </p>
                                        <div className="mt-4 flex justify-end items-center space-x-2">
                                            <p className="text-sm text-gray-500 mr-auto">
                                                由用户 ID: {invite.inviterFrom} 邀请
                                            </p>
                                            <button
                                                onClick={() => handleInvitationResponse(invite, true)}
                                                className="px-3 py-1 bg-green-500 text-white rounded hover:bg-green-600 text-sm"
                                            >
                                                同意
                                            </button>
                                            <button
                                                onClick={() => handleInvitationResponse(invite, false)}
                                                className="px-3 py-1 bg-red-500 text-white rounded hover:bg-red-600 text-sm"
                                            >
                                                拒绝
                                            </button>
                                        </div>
                                    </div>
                                ))
                            ) : (
                                <div className="text-center text-gray-500 py-8">
                                    信箱是空的
                                </div>
                            )}
                        </div>
                    </DialogContent>
                </Dialog>
            </div>
        </DndProvider>
    );
};

export default Index;