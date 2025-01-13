const path = require('path');
const webpack = require('webpack');

module.exports = {
    entry: './app/records_ui/static/details.js',  // Adjust this to your JS file
    output: {
        filename: 'bundle.js',
        path: path.resolve(__dirname, 'app/static/dist'),
    },
    module: {
        rules: [
            {
                test: /\.js$/,
                exclude: /node_modules/,
                use: {
                    loader: 'babel-loader',
                },
            },
        ],
    },
    resolve: {
        modules: [
            path.resolve(__dirname, 'node_modules'),
            'node_modules'
        ],
        alias: {
            jquery: "jquery/src/jquery",
        },
    },
    plugins: [
        new webpack.ProvidePlugin({
            $: 'jquery',  // Automatically loads jQuery whenever $ is used
            jQuery: 'jquery',  // Automatically loads jQuery whenever jQuery is used
        }),
    ],
};