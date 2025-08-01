import {Tree} from 'react-arborist';
import {ChevronDown, ChevronRight, File, FileUp, Folder, FolderPlus, Trash2} from 'lucide-react';
import {useCallback, useEffect, useState} from "react";
import VersionModal from "@/components/FileCompont/FileVersion.jsx";
import api from "@/api/axiosConfig";
import {useToast} from '@/components/Toast/useToast';
import CreateDialog from "@/components/FileCompont/CreateDialog.jsx";

const Node = ({node, style, dragHandle, tree}) => {
    const isDropTarget = node.isDropTarget && node.isInternal;
    const isSelected = node.isSelected;

    const handleDelete = (e) => {
        e.stopPropagation();
        if (window.confirm(`确定要删除 "${node.data.name}" 吗？`)) {
            tree.props.onDelete(node);
        }
    };

    const handleClick = (e) => {
        e.stopPropagation();
        node.select();
    };

    const handleDoubleClick = (e) => {
        e.stopPropagation();
        if (node.isInternal) {
            node.toggle();
        } else {
            // 对于文件，调用 activate 方法触发 onActivate 事件
            node.activate();
        }
    };

    return (
        <div
            className={`flex items-center p-2 hover:bg-gray-100 ${isDropTarget ? 'bg-blue-50' : ''} ${isSelected ? 'bg-blue-100' : ''}`}
            style={{...style, opacity: node.isDragging ? 0.5 : 1}}
            onClick={handleClick}
            onDoubleClick={handleDoubleClick}
        >
            <div
                ref={dragHandle}
                className="mr-2 cursor-move"
                onMouseDown={(e) => e.stopPropagation()}
            >⠿
            </div>
            <div className="flex items-center flex-1 cursor-pointer">
                {node.isInternal ? (
                    node.isOpen ? <ChevronDown className="h-4 w-4 mr-1"/> : <ChevronRight className="h-4 w-4 mr-1"/>
                ) : null}
                {node.isInternal ? (
                    <Folder className="h-4 w-4 mr-2 text-yellow-500"/>
                ) : (
                    <File className="h-4 w-4 mr-2 text-blue-500"/>
                )}
                <span className="truncate flex-1 select-none">{node.data.name}</span>
            </div>
            <div className="ml-auto pr-2">
                <Trash2
                    className="h-4 w-4 text-red-500 hover:text-red-700 cursor-pointer"
                    onClick={handleDelete}
                />
            </div>
        </div>
    );
};

const FileTree = ({activeChat, onModalStateChange, onFileMessageChange}) => {
    const [selectedNode, setSelectedNode] = useState(null);
    const [showVersionBox, setShowVersionBox] = useState(false);
    // 文件树数据结构
    const [fileTree, setFileTree] = useState([]);

    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [dialogMode, setDialogMode] = useState('folder');

    const {showToast, ToastContainer} = useToast();

    useEffect(() => {
        fetchFileList(true);
    }, [activeChat]);

    useEffect(() => {
        // 向父组件同步文件版本列表状态
        onModalStateChange(showVersionBox)
    }, [showVersionBox])

    // 拉取文件列表
    const fetchFileList = useCallback(async (needRefresh) => {
        if (needRefresh) {
            setFileTree([]); // 清空文件树
        }
        // 递归转换函数，将 files 和 subFolders 合并到 children
        const transformNode = (node) => {
            // 将文件映射为子节点
            const mappedFiles = (node.files || []).map(file => ({
                ...file,
                id: String(file.id),
                // 明确表示文件没有子节点
                children: null,
            }));

            // 递归转换子文件夹
            const mappedSubFolders = (node.subFolders || []).map(transformNode);

            // 合并文件夹和文件
            const combinedChildren = [...mappedSubFolders, ...mappedFiles];

            return {
                ...node,
                id: String(node.id),
                children: combinedChildren.length > 0 ? combinedChildren : [],
                files: undefined,       // 移除旧属性
                subFolders: [],  // 移除旧属性
            };
        };

        try {
            const response = await api.get(`/${activeChat}/folder/list`).catch((error) => {
                if (error.response.status === 403) {
                    showToast('无文件查看权限', 'error');
                    return null;
                } else {
                    showToast(`获取文件列表失败: ${error.message}`, 'error');
                    return null;
                }
            });
            if (response) {
                // 转换数据结构
                const transformedData = transformNode(response);
                // FileTree 需要一个数组作为根
                setFileTree(transformedData.children || []);
            } else {
                setFileTree([]);
            }
        } catch (error) {
            console.error('获取文件列表失败:', error);
        }
    }, [activeChat]);

    // 处理文件或文件夹删除
    const handleDeleteNode = useCallback(async (node) => {
        try {
            if (node.isInternal) {
                // 删除文件夹
                await api.delete(`/${activeChat}/folder/remove`, {
                    data: {
                        folderId: node.id
                    }
                });
            } else {
                // 删除文件
                await api.delete(`/${activeChat}/file/remove`, {
                    data: {
                        fileId: node.id
                    }
                });
            }
            // 成功后刷新文件列表
            await fetchFileList(true);
            showToast(`"${node.data.name}" 删除成功`, 'success');
        } catch (error) {
            showToast(`删除失败: ${error.message}`, 'error');
        }
    }, [activeChat, fetchFileList]);

    // 处理文件选择
    const handleFileSelect = useCallback((nodes) => {
        if (nodes.length > 0) {
            setSelectedNode(nodes[0].data);
            // 移除自动显示版本弹窗的逻辑，改为双击时触发
        } else {
            // 取消选中
            setSelectedNode(null);
            setShowVersionBox(false);
        }
    }, []);

    // 添加双击处理函数
    const handleNodeDoubleClick = useCallback((node) => {
        console.log(node)
        if (!node.isInternal) {
            // 双击文件时显示版本弹窗
            setShowVersionBox(true);
        }
    }, []);

    // 处理文件移动
    const handleFileMove = useCallback((arg) => {
        const dragIds = arg.dragIds[0];
        const parentId = arg.parentId == null ? 0 : arg.parentId;
        const dragNodes = arg.dragNodes[0];

        const targetName = dragNodes.data.name;
        const sourcePath = [];
        const targetPath = [];

        // 客户端先自行更新文件树
        setFileTree(currentTree => {
            const newTree = JSON.parse(JSON.stringify(currentTree));

            // dfs 搜索结点
            const removeNode = (nodes, nodeId) => {
                for (let i = 0; i < nodes.length; i++) {
                    if (nodes[i].id === nodeId) {
                        nodes.splice(i, 1);
                        return true;
                    }
                    if (nodes[i].children) {
                        sourcePath.push(nodes[i].name); // 添加该结点
                        if (removeNode(nodes[i].children, nodeId)) return true;
                        sourcePath.pop(); // 恢复该结点
                    }
                }
                return false;
            };

            // Helper to find the parent and add the node
            const addNode = (nodes, targetParentId, nodeToAdd) => {
                if (targetParentId === null) { // Dropped at the root
                    nodes.push(nodeToAdd.data);
                    return true;
                }
                for (const node of nodes) {
                    if (node.id === targetParentId) {
                        if (!node.children) node.children = [];
                        node.children.push(nodeToAdd.data);
                        targetPath.push(node.name); // 添加该结点
                        return true;
                    }
                    if (node.children) {
                        targetPath.push(node.name); // 添加该结点
                        if (addNode(node.children, targetParentId, nodeToAdd)) return true;
                        targetPath.pop(); // 恢复该结点
                    }
                }
                return false;
            };

            removeNode(newTree, dragNodes.id);
            addNode(newTree, parentId, dragNodes);

            return newTree;
        });


        // 发送更新请求
        try {
            let source = '/' + sourcePath.join('/') + '/' + targetName;
            let target = '/' + targetPath.join('/') + '/' + targetName;

            if (!dragNodes.isInternal) {
                // 拖动为文件
                api.post(`/${activeChat}/file/move`, {
                    fileId: dragIds,
                    preFolderId: parentId
                }).then(() => {
                    // 成功后重新拉取文件列表
                    fetchFileList(false);

                    // 通知父组件
                    onFileMessageChange({
                        type: 3,
                        source: source,
                        target: target,
                    })
                }).catch((error) => {
                    if (error.response.status === 403) {
                        showToast('无操作权限', error)
                    } else {
                        showToast(`文件夹移动失败: ${error.message}`, 'error');
                    }
                    fetchFileList(false);
                });
            } else {
                // 拖动为文件夹
                api.post(`/${activeChat}/folder/move`, {
                    folderId: dragIds,
                    preFolderId: parentId
                }).then(() => {
                    // 成功后重新拉取文件列表
                    fetchFileList(false);

                    // 通知父组件
                    onFileMessageChange({
                        type: 4,
                        source: source,
                        target: target,
                    })
                }).catch((error) => {
                    if (error.response.status === 403) {
                        showToast('无操作权限', error)
                    } else {
                        showToast(`文件夹移动失败: ${error.message}`, 'error');
                    }
                    fetchFileList(false);
                });
            }
        } catch (error) {
            fetchFileList(false);
            console.error('文件移动失败:', error);
        }
    }, [activeChat, fetchFileList]);

    // 处理文件上传
    const handleFileUpload = useCallback(async (files, comment) => {
        let parentId = 0;
        if (selectedNode) {
            parentId = selectedNode.children !== null ? selectedNode.id : selectedNode.folderId;
        }

        const uploadPromises = Array.from(files).map(async (file) => {
            try {
                // 第一步：获取预签名上传URL
                const urlResponse = await api.post(`/${activeChat}/file/upload/getUrl`, {
                    fileName: file.name,
                    preFolderId: parentId
                });

                const {url: uploadUrl, objectName} = urlResponse;

                // 第二步：直接上传文件到OBS
                const uploadResponse = await fetch(uploadUrl, {
                    method: 'PUT',
                    body: file,
                    headers: {
                        'Content-Type': file.type || 'application/octet-stream'
                    }
                });

                if (!uploadResponse.ok) {
                    throw new Error(`上传失败: ${uploadResponse.status} ${uploadResponse.statusText}`);
                }

                // 构造文件URL（基于OBS响应或预定义格式）
                const fileUrl = uploadUrl.split('?')[0]; // 移除查询参数获取文件URL

                // 第三步：确认上传到后端
                await api.post(`/${activeChat}/file/upload/confirm`, {
                    fileName: file.name,
                    fileUrl: fileUrl,
                    preFolderId: parentId,
                    comment: comment || ''
                });

                return {success: true, fileName: file.name};

            } catch (error) {
                console.error(`文件 ${file.name} 上传失败:`, error);
                throw new Error(`${file.name}: ${error.message}`);
            }
        });

        try {
            showToast('文件开始上传', 'info');

            // 并行处理所有文件上传
            const results = await Promise.all(uploadPromises);

            // 刷新文件列表
            await fetchFileList(true);

            // 通知父组件
            onFileMessageChange({type: 2, files: files});

            showToast(`${results.length} 个文件上传成功`, 'success');

        } catch (error) {
            showToast(`文件上传失败: ${error.message}`, 'error');
            console.error('文件上传失败:', error);
            throw error; // 抛出错误以在对话框中处理
        }
    }, [activeChat, selectedNode, fetchFileList, onFileMessageChange, showToast]);

    // 新建文件夹处理
    const handleCreateFolder = useCallback(async (folderName) => {
        let parentId = 0;
        if (selectedNode) {
            parentId = selectedNode.children !== null ? selectedNode.id : selectedNode.folderId;
        }

        try {
            await api.post(`/${activeChat}/folder/create`, {
                name: folderName,
                parentId: parentId,
            });
            await fetchFileList(true);
            onFileMessageChange({type: 1, folderName: folderName});
            showToast(`文件夹 "${folderName}" 创建成功`, 'success');
        } catch (error) {
            showToast(`创建文件夹失败: ${error.message}`, 'error');
            console.error('创建文件夹失败:', error);
            throw error; // 抛出错误
        }
    }, [activeChat, selectedNode, fetchFileList, onFileMessageChange, showToast]);

    // 处理对话框提交
    const handleDialogSubmit = async (data) => {
        if (dialogMode === 'folder') {
            await handleCreateFolder(data.name);
        } else {
            await handleFileUpload(data.files, data.comment);
        }
    };

    const handleDrop = (files, parentId) => {
        // console.log(files, parentId);
        // const newFiles = Array.from(files).map(file => ({
        //     id: `file-${Date.now()}-${file.name}`,
        //     name: file.name,
        //     type: 'file',
        //     size: file.size,
        //     date: new Date().toISOString().split('T')[0]
        // }));
        // handleFileUpload(newFiles, parentId);
    };

    return (
        <div className="h-full overflow-y-auto">
            <ToastContainer/>
            <div className="p-4 flex items-center justify-between">
                <h3 className="text-lg font-medium">会话文件</h3>
                <div className="flex items-center">
                    <button
                        onClick={() => {
                            setDialogMode('folder');
                            setIsDialogOpen(true);
                        }}
                        className="mr-3 cursor-pointer text-blue-500 hover:text-blue-700 flex items-center p-1 rounded"
                        title="新建文件夹"
                    >
                        <FolderPlus className="h-5 w-5 mr-1"/>
                    </button>
                    <button
                        onClick={() => {
                            setDialogMode('file');
                            setIsDialogOpen(true);
                        }}
                        className="mr-3 cursor-pointer text-blue-500 hover:text-blue-700 flex items-center p-1 rounded"
                        title="上传文件"
                    >
                        <FileUp className="h-5 w-5 mr-1"/>
                    </button>
                </div>
            </div>
            <Tree
                data={fileTree}
                idAccessor="id"
                childrenAccessor="children"
                openByDefault={false}
                width="100%"
                height={600}
                indent={24}
                rowHeight={36}
                onMove={handleFileMove}
                onSelect={handleFileSelect}
                onDelete={handleDeleteNode}
                onActivate={handleNodeDoubleClick} // 双击事件
            >
                {Node}
            </Tree>

            {/* 版本信息弹窗 */}
            <VersionModal
                file={selectedNode}
                open={showVersionBox}
                onOpenChange={setShowVersionBox}
                activeChat={activeChat}
            />

            {/* 新建/上传对话框 */}
            <CreateDialog
                isOpen={isDialogOpen}
                onClose={() => setIsDialogOpen(false)}
                onSubmit={handleDialogSubmit}
                mode={dialogMode}
            />
        </div>
    );
};

export default FileTree;