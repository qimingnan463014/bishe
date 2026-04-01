const fs = require('fs');
const files = ['src/main/resources/static/admin/index.html', 'src/main/resources/static/home.html'];

files.forEach(f => {
    let txt = fs.readFileSync(f, 'utf8');
    txt = txt.replace(/<(el-[a-z-]+|[A-Z][a-zA-Z]*)([^>]*?)\/>/g, '<$1$2></$1>');
    fs.writeFileSync(f, txt);
    console.log('Fixed', f);
});
