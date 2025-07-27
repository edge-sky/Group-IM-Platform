import React, { useState, useEffect } from 'react';
import { X, CheckCircle, AlertCircle, Info, AlertTriangle } from 'lucide-react';

const Toast = ({
                   message,
                   type = 'info',
                   duration = 3000,
                   onClose,
                   show = false
               }) => {
    const [isVisible, setIsVisible] = useState(show);

    useEffect(() => {
        setIsVisible(show);
    }, [show]);

    useEffect(() => {
        if (isVisible && duration > 0) {
            const timer = setTimeout(() => {
                handleClose();
            }, duration);

            return () => clearTimeout(timer);
        }
    }, [isVisible, duration]);

    const handleClose = () => {
        setIsVisible(false);
        setTimeout(() => {
            onClose && onClose();
        }, 300); // 等待动画完成
    };

    const getToastStyle = () => {
        const baseStyle = "flex items-center p-4 rounded-lg shadow-lg border";

        switch (type) {
            case 'success':
                return `${baseStyle} bg-green-50 border-green-200 text-green-800`;
            case 'error':
                return `${baseStyle} bg-red-50 border-red-200 text-red-800`;
            case 'warning':
                return `${baseStyle} bg-yellow-50 border-yellow-200 text-yellow-800`;
            default:
                return `${baseStyle} bg-blue-50 border-blue-200 text-blue-800`;
        }
    };

    const getIcon = () => {
        const iconClass = "h-5 w-5 mr-3 flex-shrink-0";

        switch (type) {
            case 'success':
                return <CheckCircle className={`${iconClass} text-green-600`} />;
            case 'error':
                return <AlertCircle className={`${iconClass} text-red-600`} />;
            case 'warning':
                return <AlertTriangle className={`${iconClass} text-yellow-600`} />;
            default:
                return <Info className={`${iconClass} text-blue-600`} />;
        }
    };

    if (!isVisible) return null;

    return (
        <div
            className={`
            fixed top-4 left-1/2 transform -translate-x-1/2 z-[9999]
            transition-all duration-300 ease-in-out max-w-md w-full mx-4
            ${isVisible ? 'opacity-100 translate-y-0' : 'opacity-0 -translate-y-2'}
        `}
        >
            <div className={getToastStyle()}>
                {getIcon()}
                <span className="flex-1 text-sm font-medium">{message}</span>
                <button
                    onClick={handleClose}
                    className="ml-3 p-1 rounded-full hover:bg-black hover:bg-opacity-10 transition-colors"
                >
                    <X className="h-4 w-4" />
                </button>
            </div>
        </div>
    );
};

export default Toast;