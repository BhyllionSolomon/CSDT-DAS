import Sidebar from "./Sidebar";
import Navbar from "./Navbar";

import { Outlet } from "react-router-dom";

function Layout() {

    return (

        <div className="app">

            <Sidebar />

            <div className="main">

                <Navbar />

                <div className="content">

                    <Outlet />

                </div>

            </div>

        </div>

    );

}

export default Layout;