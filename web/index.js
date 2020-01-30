import React from "react";
import ReactDOM from "react-dom";
import App from './app/app';
import 'bootstrap';
import 'bootstrap/dist/css/bootstrap.css';
import "./index.styl";

let mountNode = document.getElementById("app");
ReactDOM.render(<App />, mountNode);