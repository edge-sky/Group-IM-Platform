import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog.jsx';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table.jsx';
import {useEffect, useState} from "react";
import api from "@/api/axiosConfig";

const VersionModal = ({ file, open, onOpenChange, activeChat }) => {
    if (!file) return null;

    const [versions, setVersions] = useState([]);
    const [userInfo, setUserInfo] = useState(new Map());

    const fetchVersions = async () => {
        api.get(`/${activeChat}/file/versionList/${file.id}`).then(
            response => {
                setVersions(response)
            }
        );

        api.get(`${activeChat}/conversation/userInfo`).then(response => {
            const userMap = new Map();
            response.forEach(user => {
                userMap.set(user.uid, user.username);
            })
            setUserInfo(userMap);
        });
    }

    useEffect(() => {
        if (open) {
            fetchVersions();
        }
    }, [open])

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="sm:max-w-3xl">
                <DialogHeader>
                    <DialogTitle className="text-xl">文件版本 - {file.name}</DialogTitle>
                </DialogHeader>
                <div className="mt-4">
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead>版本</TableHead>
                                <TableHead>修改人</TableHead>
                                <TableHead>修改日期</TableHead>
                                <TableHead>备注</TableHead>
                                <TableHead className="text-right">操作</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {versions.map((version, index) => (
                                <TableRow key={index}>
                                    <TableCell className="font-medium">{version.version}</TableCell>
                                    <TableCell>{userInfo.get(version.updateUid)}</TableCell>
                                    <TableCell>{version.createTime}</TableCell>
                                    <TableCell>{version.comment}</TableCell>
                                    <TableCell className="text-right">
                                        <button className="text-blue-500 hover:text-blue-700" onClick={() => window.open(version.fileUrl, '_blank')}>
                                            下载
                                        </button>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                </div>
            </DialogContent>
        </Dialog>
    );
};

export default VersionModal;