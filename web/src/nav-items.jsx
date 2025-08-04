import { HomeIcon, MessageSquare, LogInIcon } from "lucide-react";
import Index from "./pages/Index.jsx";
import AuthPage from "./pages/AuthPage.jsx";

/**
* Central place for defining the navigation items. Used for navigation components and routing.
*/
export const navItems = [
  {
    title: "首页",
    to: "/",
    icon: <HomeIcon className="h-4 w-4" />,
    page: <Index />,
  },
  {
    title: "消息",
    to: "/messages",
    icon: <MessageSquare className="h-4 w-4" />,
    page: <Index />,
  },
  {
    title: "登录",
    to: "/auth",
    icon: <LogInIcon className="h-4 w-4" />,
    page: <AuthPage />,
  },
];
