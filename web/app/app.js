import React from 'react';
import axios from 'axios';
import "./app.styl";

import f5_logo from '../assets/f5_logo.png';
import engine_logo from '../assets/engine_logo.png';

class App extends React.Component {
    constructor(props){
        super(props);
        this.state = {
            f5MockJson: "Loading...",
            payloadJson: "",
            results: []
        };

        this.fetchF5Mock();
    }

     async fetchF5Mock() {
        let result = await axios.get(window.location.origin + '/api/f5Config');
        this.setState({f5MockJson: JSON.stringify(result.data, null, 4)});
    };

    render() {
        return (
            <div className="app">
                <h1>Operation Engine user interface!</h1>
                <div className="container bg-dark contents">
                    <div className="card">
                        <div className="card-img-wrapper">
                            <img className="card-img-top" src={f5_logo} alt="F5 logo" />
                        </div>
                        <div className="card-body">
                            <textarea className="form-control" value={this.state.f5MockJson} />
                        </div>
                    </div>

                    <div className="card">
                        <div className="card-img-wrapper">
                            <img className="card-img-top" src={engine_logo} alt="Engine logo" />
                        </div>
                        <div className="card-body with-buttons">
                            <textarea  className="form-control">This text area should contain operation results</textarea>
                            <div className="btn-group">
                                <button type="button" className="btn btn-primary">Copy</button>
                                <button type="button" className="btn btn-primary">Clear</button>
                                <button type="button" className="btn btn-primary">Execute</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}

export default App;