const path = require('path');
const webpack = require('webpack');

const targetPath = path.join(
  __dirname,
  '..',
  '..',
  '..',
  'target',
  'test-classes',
  'ts'
);

const srcPath = path.join(__dirname, 'src');


module.exports = function() {
  const serverJs = true;
  const authorMode = false;

  const target = 'web';
  const entries = {};

  var files = [];
    files.push(path.join(srcPath, 'server.tsx'));
    entries.reactserver = files;


  console.log("target '" + targetPath);



  const config = {
    entry: entries,
    target: target,
    output: {
      path: targetPath,
      filename: '[name].js'
    },
    resolve: {
      alias: {
        react: path.resolve('node_modules/react'),
        'react-dom': path.resolve('node_modules/react-dom')
      },
      extensions: ['.ts', '.tsx', '.webpack.js', '.web.js', '.js']
    },
    module: {
      rules: [
        {
          test: /\.tsx?$/,
          use: [{loader: 'ts-loader'}]
        }
      ]
    },
    plugins: [
      new webpack.LoaderOptionsPlugin({
        debug: true
      })
    ]
  };



  return config;
};
