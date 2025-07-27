import React, { useState, useRef, useEffect } from 'react';
import { X, Upload, FolderPlus } from 'lucide-react';

const CreateDialog = ({ isOpen, onClose, onSubmit, mode }) => {
    const [name, setName] = useState('');
    const [files, setFiles] = useState([]);
    const [comment, setComment] = useState('');
    const [isProcessing, setIsProcessing] = useState(false);
    const fileInputRef = useRef(null);

    useEffect(() => {
        if (!isOpen) {
            // 关闭时重置状态
            setName('');
            setFiles([]);
            setComment('');
            if (fileInputRef.current) {
                fileInputRef.current.value = '';
            }
        }
    }, [isOpen]);

    const handleFileChange = (e) => {
        if (e.target.files.length > 0) {
            setFiles(Array.from(e.target.files));
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsProcessing(true);
        try {
            if (mode === 'folder') {
                await onSubmit({ name });
            } else {
                await onSubmit({ files, comment });
            }
            onClose(); // 成功后关闭
        } catch (error) {
            console.error(`${mode === 'folder' ? '创建' : '上传'}失败:`, error);
        } finally {
            setIsProcessing(false);
        }
    };

    if (!isOpen) return null;

    const isSubmitDisabled = isProcessing ||
        (mode === 'folder' ? !name.trim() : (files.length === 0 || !comment.trim()));
    const title = mode === 'folder' ? '新建文件夹' : '上传文件';
    const buttonText = mode === 'folder' ? '创建' : '上传';
    const ButtonIcon = mode === 'folder' ? FolderPlus : Upload;

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg p-6 w-96 max-w-full">
                <div className="flex justify-between items-center mb-4">
                    <h3 className="text-lg font-semibold">{title}</h3>
                    <button onClick={onClose} className="text-gray-500 hover:text-gray-700">
                        <X className="h-5 w-5" />
                    </button>
                </div>

                <form onSubmit={handleSubmit}>
                    {mode === 'folder' ? (
                        <div className="mb-4">
                            <label className="block text-sm font-medium text-gray-700 mb-2">文件夹名称</label>
                            <input
                                type="text"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="请输入文件夹名称"
                                className="w-full p-2 border border-gray-300 rounded-md"
                                required
                                autoFocus
                            />
                        </div>
                    ) : (
                        <>
                            <div className="mb-4">
                                <label className="block text-sm font-medium text-gray-700 mb-2">选择文件</label>
                                <div className="flex items-center justify-center w-full">
                                    <label className="flex flex-col items-center justify-center w-full h-32 border-2 border-gray-300 border-dashed rounded-lg cursor-pointer bg-gray-50 hover:bg-gray-100">
                                        <div className="flex flex-col items-center justify-center pt-5 pb-6">
                                            <Upload className="w-8 h-8 mb-3 text-gray-400" />
                                            <p className="mb-2 text-sm text-gray-500">
                                                <span className="font-semibold">点击选择文件</span> 
                                            </p>
                                        </div>
                                        <input ref={fileInputRef} type="file" className="hidden" multiple onChange={handleFileChange} />
                                    </label>
                                </div>
                                {files.length > 0 && (
                                    <div className="mt-2 text-sm text-gray-600">
                                        已选择 {files.length} 个文件:
                                        <ul className="max-h-20 overflow-y-auto list-disc list-inside">
                                            {files.map((file, index) => <li key={index} className="truncate">{file.name}</li>)}
                                        </ul>
                                    </div>
                                )}
                            </div>
                            <div className="mb-4">
                                <label className="block text-sm font-medium text-gray-700 mb-2">备注信息</label>
                                <textarea
                                    value={comment}
                                    onChange={(e) => setComment(e.target.value)}
                                    placeholder="请输入文件备注信息..."
                                    className="w-full p-2 border border-gray-300 rounded-md resize-none h-20"
                                    required
                                />
                            </div>
                        </>
                    )}

                    <div className="flex justify-end space-x-3">
                        <button type="button" onClick={onClose} className="px-4 py-2 text-gray-600 border border-gray-300 rounded-md hover:bg-gray-50" disabled={isProcessing}>
                            取消
                        </button>
                        <button type="submit" disabled={isSubmitDisabled} className="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 disabled:opacity-50 flex items-center">
                            {isProcessing ? (
                                <>
                                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                                    处理中...
                                </>
                            ) : (
                                <>
                                    <ButtonIcon className="h-4 w-4 mr-2" />
                                    {buttonText}
                                </>
                            )}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default CreateDialog;
