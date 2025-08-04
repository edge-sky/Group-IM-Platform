import { useState } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { LoginForm } from "./LoginForm.jsx";
import { RegisterForm } from "./RegisterForm.jsx";
import { Dialog, DialogContent } from "@/components/ui/dialog";

export const AuthModal = ({ open, onOpenChange }) => {
  const [activeTab, setActiveTab] = useState("login");

  const handleLogin = (data) => {
    console.log("Login data:", data);
    // 这里添加登录逻辑
    onOpenChange(false);
  };

  const handleRegister = (data) => {
    console.log("Register data:", data);
    // 这里添加注册逻辑
    setActiveTab("login");
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[425px]">
        <Tabs
          value={activeTab}
          onValueChange={setActiveTab}
          className="w-full mt-4"
        >
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="login">登录</TabsTrigger>
            <TabsTrigger value="register">注册</TabsTrigger>
          </TabsList>
          <TabsContent value="login">
            <LoginForm onSubmit={handleLogin} />
          </TabsContent>
          <TabsContent value="register">
            <RegisterForm onSubmit={handleRegister} />
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
};
