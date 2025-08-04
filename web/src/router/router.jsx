import { navItems } from "../nav-items";
import { createBrowserRouter } from "react-router-dom";

const routes = navItems.map(item => ({
    path: item.to,
    element: item.page,
}));

export const router = createBrowserRouter(routes);
