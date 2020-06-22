import React from 'react';
import Rest from '../services/rest';
import "./app.styl";

import f5_logo from '../assets/f5_logo.png';
import engine_logo from '../assets/engine_logo.png';

class App extends React.Component {

    constructor(props){
        super(props);
        this.state = {
            f5MockJson: "Loading...",
            payloadJson: "",
            payloadValid: true,
            uiLocked: false
        };

        this.fetchF5Mock();
    }

     async fetchF5Mock() {
        let result = await Rest.get('/operations/f5Config');
        this.setState({
            f5MockJson: JSON.stringify(result.data, null, 4)
        });
    }

    updateConfig(event){
        this.setState({
            payloadJson: event.target.value
        });
    }

    copyConfig(){
        this.setState({
            payloadJson: this.state.f5MockJson
        });
    }

    clearConfig(){
        this.setState({
            payloadJson: "",
            payloadValid: true
        });
    }

    async execute(){
        let payloadString = this.state.payloadJson;
        let isConfigValid = this.validateConfig(payloadString);
        if(isConfigValid) {
            this.setState({
                payloadValid: true,
                uiLocked: true
            });

            let payloadJson = JSON.parse(payloadString);
            payloadJson.results = {};
            let result = await Rest.post('/operations/execute', payloadJson);

            console.log("execution result:", result.data);
            this.setState({
                payloadJson: JSON.stringify(result.data, null, 4),
                uiLocked: false
            });
        }else{
            this.setState({
                payloadValid: false
            });
            console.log("failed to parse execution payload");
        }
    }

    validateConfig(jsonString){
        try{
            let parsedConfig = JSON.parse(jsonString);
            return parsedConfig.hasOwnProperty("administrativeConfig")
                && parsedConfig.hasOwnProperty("operation")
                && parsedConfig.hasOwnProperty("results");
        }catch(e){
            return false;
        }
    }

    async terminate(){
        let runningOperations = await Rest.get('/diagnostics/operations');
        console.log("running operations: ", runningOperations);

        let terminationResults = await Rest.post("/operations/terminate", runningOperations.data);
        console.log("termination results: ", terminationResults);
    }

    render() {
        return (
            <div className="app">
                <h1>Operation Engine user interface</h1>
                <div className="container bg-dark contents">
                    <div className="card">
                        <div className="card-img-wrapper">
                            <img className="card-img-top" src={f5_logo} alt="F5 logo" />
                        </div>
                        <div className="card-body">
                            <textarea disabled className="form-control" value={this.state.f5MockJson} />
                        </div>
                    </div>

                    <div className="card">
                        <div className="card-img-wrapper">
                            <img className="card-img-top" src={engine_logo} alt="Engine logo" />
                        </div>
                        <div className={this.state.payloadValid? "card-body with-buttons" : "card-body with-buttons border-danger"}>
                            <textarea disabled={this.state.uiLocked} className="form-control" placeholder="This text area should contain operation results" value={this.state.payloadJson} onChange={this.updateConfig.bind(this)} />
                            <div className="btn-group">
                                <button type="button" className="btn btn-primary" disabled={this.state.uiLocked} onClick={this.copyConfig.bind(this)}>Copy</button>
                                <button type="button" className="btn btn-primary" disabled={this.state.uiLocked} onClick={this.clearConfig.bind(this)}>Clear</button>
                                {
                                    this.state.uiLocked?
                                    <button type="button" className="btn btn-primary" disabled>
                                        <span className="spinner-border spinner-border-sm" />
                                        <span>Running...</span>
                                    </button>:
                                    <button type="button" className="btn btn-primary" onClick={this.execute.bind(this)}>Execute</button>
                                }
                                <button type="button" className="btn btn-primary" disabled={!this.state.uiLocked} onClick={this.terminate.bind(this)}>Terminate</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}

export default App;