module.exports = {
    dev: {
        remotes: [{
            host: '172.31.XXX.XXX',
            username: 'user',
            password: 'pass',
            port: 22
        }],
        basePath: '/sixsense/'
    },

    ansible: {
        remotes: [{
            host: '172.31.XXX.XXX',
            username: 'user',
            password: 'pass',
            port: 22
        }],
        basePath: '/ansible/'
    },

    rabbit: {
        remotes: [{
            host: '172.31.XXX.XXX',
            username: 'user',
            password: 'pass',
            port: 5672,
            vhost: '/xxx'
        }],
        basePath: '/rabbit/'
    }
};