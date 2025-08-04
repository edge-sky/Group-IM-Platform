import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { LoginForm } from "@/components/Auth/LoginForm";
import { RegisterForm } from "@/components/Auth/RegisterForm";
import {useState} from "react";
import { useToast } from '@/components/Toast/useToast';

const Index = () => {
  const [loginTap, setLoginTab] = useState(true);
  const { showToast, ToastContainer } = useToast();

  const handleRegisterSuccess = () => {
    showToast('注册成功，请登录！', 'success');
    setLoginTab(true); // 切换到登录标签页
  };

  return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50 p-4">
        {/* Toast容器 */}
        <ToastContainer />
        <div className="w-full max-w-md bg-white rounded-lg shadow-md overflow-hidden">
          <Tabs
              value={loginTap ? 'login' : 'register'}
              onValueChange={(value) => setLoginTab(value === 'login')}
              className="w-full"
          >
            <TabsList className="grid w-full grid-cols-2 rounded-none">
              <TabsTrigger value="login">登录</TabsTrigger>
              <TabsTrigger value="register">注册</TabsTrigger>
            </TabsList>
            <div className="p-6">
              <TabsContent value="login">
                <div className="space-y-4">
                  <h2 className="text-2xl font-bold text-center">用户登录</h2>
                  <LoginForm />
                </div>
              </TabsContent>
              <TabsContent value="register">
                <div className="space-y-4">
                  <h2 className="text-2xl font-bold text-center">用户注册</h2>
                  <RegisterForm onRegisterSuccess={handleRegisterSuccess} />
                </div>
              </TabsContent>
            </div>
          </Tabs>
        </div>
      </div>
  );
};

export default Index;