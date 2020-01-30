import React from 'react';
import 'bootstrap';
import 'bootstrap/dist/css/bootstrap.css'
import "./app.styl"

class App extends React.Component {
    render() {
        return (
            <div className="app">
                <h1>Operation Engine user interface!</h1>
                <div className="container bg-dark contents">
                    <div className="col">
                        <textarea>This text area should contain sample raw execution configs</textarea>
                    </div>
                    <div className="col">
                        <textarea>This text area should contain operation results</textarea>
                    </div>
                </div>
            </div>
        );
    }
}

export default App;