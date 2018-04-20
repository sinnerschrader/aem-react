/*eslint no-console: 0*/

var path = require('path');
var fs = require('fs');
var webpack = require('webpack');

const configFn = function(env) {
    const serverJs = env.server || false;
    const watch = env.watch || false;
    process.env.NODE_ENV = env.production ? 'production' : 'development';

    console.log("watch = " + watch);
    var jcrPath = path.join(__dirname, '..', '..', '..', 'target', 'classes', 'etc', 'designs', 'react-demo', 'js', 'react-demo');


    var targetFileName = serverJs ? "server.js" : "app.js";

    console.log("Webpack build for '" + process.env.NODE_ENV + "' -> " + targetFileName);
    console.log("jcrpath '" + jcrPath);


    var target = "web";
    var entries = {};
    if (!serverJs) {
        entries.app = './src/client.tsx';
    } else {
        entries.server = ['babel-polyfill','./src/server.tsx'];
    }

//  env==production : prevents spurious error in nashorn when checking: typeof instance.receiveComponent === 'function'
    var env = '"' + process.env.NODE_ENV + '"';// '"' + (serverJs ? "production" : process.env.NODE_ENV) + '"';

    console.log("env " + env)

    var plugins = [
        new webpack.DefinePlugin({
            'process.env': {
                'NODE_ENV': env
            }
        })];

    var config = {
        entry: entries,
        target: target,
        output: {
            path: jcrPath,
            filename: "[name].js"
        },
        resolve: {
            extensions: ['.tsx', '.webpack.js', '.web.js', '.js']

        },
        module: {
            loaders: [
                {
                    test: /\.(ts|tsx)$/,
                    loader: 'ts-loader'

                }
            ]
        },
        plugins: plugins

    };

    if (process.env.NODE_ENV === 'production') {
        config.plugins.push(new webpack.optimize.UglifyJsPlugin());
    } else {
        config.devtool = 'inline-source-map';
    }
    return config;
}
module.exports = configFn;

