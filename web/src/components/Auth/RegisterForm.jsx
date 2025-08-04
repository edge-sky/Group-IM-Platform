import {Lock, User} from "lucide-react";
import {Button} from "@/components/ui/button";
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import api from '@/api/axiosConfig';
import {useForm} from "react-hook-form";

export const RegisterForm = ({ onRegisterSuccess }) => {
        const {
            register,
            handleSubmit,
            watch,
            formState: {errors},
        } = useForm();

        const onSubmit = async (data) => {
            try {
                // 后端需要 { username, password } 格式的数据
                await api.post('/user/register', {
                    username: data.username,
                    password: data.password
                });
                // 调用从父组件传递过来的成功回调函数
                if (onRegisterSuccess) {
                    onRegisterSuccess();
                }
            } catch (error) {
                // 处理错误响应
                const errorMessage = error.response?.data?.message || '注册失败，请稍后再试。';
                console.error('注册失败:', errorMessage);
                alert(errorMessage);
            }
        };

        return (
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                <div className="space-y-2">
                    <Label htmlFor="username">用户名</Label>
                    <div className="relative">
                        <User className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400"/>
                        <Input
                            id="username"
                            placeholder="请输入用户名"
                            className="pl-10"
                            {...register("username", {required: "用户名不能为空"})}
                        />
                    </div>
                    {errors.username && (
                        <p className="text-sm text-red-500">{errors.username.message}</p>
                    )}
                </div>

                <div className="space-y-2">
                    <Label htmlFor="password">密码</Label>
                    <div className="relative">
                        <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400"/>
                        <Input
                            id="password"
                            type="password"
                            placeholder="请输入密码"
                            className="pl-10"
                            {...register("password", {
                                required: "密码不能为空",
                                minLength: {
                                    value: 6,
                                    message: "密码至少需要6个字符",
                                },
                            })}
                        />
                    </div>
                    {errors.password && (
                        <p className="text-sm text-red-500">{errors.password.message}</p>
                    )}
                </div>

                <div className="space-y-2">
                    <Label htmlFor="confirmPassword">确认密码</Label>
                    <div className="relative">
                        <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400"/>
                        <Input
                            id="confirmPassword"
                            type="password"
                            placeholder="请再次输入密码"
                            className="pl-10"
                            {...register("confirmPassword", {
                                required: "请确认密码",
                                validate: (value) =>
                                    value === watch("password") || "密码不匹配",
                            })}
                        />
                    </div>
                    {errors.confirmPassword && (
                        <p className="text-sm text-red-500">{errors.confirmPassword.message}</p>
                    )}
                </div>

                <Button type="submit" className="w-full">
                    注册
                </Button>
            </form>
        );
    }
;