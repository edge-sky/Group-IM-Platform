import { useForm } from "react-hook-form";
import { Lock, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {useNavigate} from "react-router-dom";
import api from "@/api/axiosConfig";

export const LoginForm = () => {
    const navigate = useNavigate();

    const {
        register,
        handleSubmit,
        formState: { errors },
    } = useForm();

    const onSubmit = async (data) => {
        try {
            const response = await api.post("/user/login", {
                username: data.username,
                password: data.password,
            })

            if (response) {
                navigate("/");
            } else {
                alert("登录失败，请检查账号或密码！");
            }
        } catch (error) {
            console.error("登录错误:", error);
            alert("登录失败，请稍后重试！");
        }
    };

    return (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="space-y-2">
                <Label htmlFor="username">账号</Label>
                <div className="relative">
                    <User className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                    <Input
                        id="username"
                        placeholder="请输入账号"
                        className="pl-10"
                        {...register("username", { required: "账号不能为空" })}
                    />
                </div>
                {errors.username && (
                    <p className="text-sm text-red-500">{errors.username.message}</p>
                )}
            </div>

            <div className="space-y-2">
                <Label htmlFor="password">密码</Label>
                <div className="relative">
                    <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                    <Input
                        id="password"
                        type="password"
                        placeholder="请输入密码"
                        className="pl-10"
                        {...register("password", { required: "密码不能为空" })}
                    />
                </div>
                {errors.password && (
                    <p className="text-sm text-red-500">{errors.password.message}</p>
                )}
            </div>

            <Button type="submit" className="w-full">
                登录
            </Button>
        </form>
    );
};