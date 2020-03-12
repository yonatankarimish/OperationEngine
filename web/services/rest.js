import axios from 'axios';

class Rest {
    static async get(path) {
        let url = Rest.normalize(path);
        return await axios.get(url).catch(Rest.handleError);
    }

    static async post(path, payload){
        let url = Rest.normalize(path);
        return await axios.post(url, payload).catch(Rest.handleError);
    }

    static async put(path, payload){
        let url = Rest.normalize(path);
        return await axios.put(url, payload).catch(Rest.handleError);
    }

    static async delete(path){
        let url = Rest.normalize(path);
        return await axios.delete(url).catch(Rest.handleError);
    }

    static handleError(response){
        console.log(response);
    }

    static normalize(partialPath){
        return window.location.origin + '/api' + partialPath;
    }
}

export default Rest;