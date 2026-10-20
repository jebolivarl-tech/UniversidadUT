#!/usr/bin/env bash
# Verificación end-to-end de la API local contra una base de pruebas real (MySQL).
#
#   cd UniversidadDao
#   bash scripts/preparar-base-pruebas.sh                      # base universidad_e2e + 3 cuentas
#   DB_URL="jdbc:mysql://localhost:3306/universidad_e2e" DB_USER=root DB_PASSWORD=su-clave \
#     java -jar target/app.jar                                 # en otra ventana
#   bash scripts/verificar-api-e2e.sh                           # 202 comprobaciones, sale 0 si todo OK
#
# Variables por si su entorno es distinto:
#   BASE_URL               por defecto http://localhost:8080
#   ADMIN_USR/ADMIN_CLAVE  administrativo de la base de pruebas  (e2e_admin / ClaveAdmin123)
#   PROF_USR/PROF_CLAVE    profesor con ficha                    (e2e_prof  / ClaveProfesor1)
#   EST_USR/EST_CLAVE      estudiante con ficha                  (e2e_ana   / ClaveEstudiante1)
#
# Cubre las reglas de docs/reglas-de-negocio.md (sección decide curso y periodo, periodo abierto,
# carrera, cupo, 20 créditos, 30/30/40, 75 % de asistencia, nota propagada a la matrícula), la matriz
# de permisos por rol, las validaciones de forma, los filtros, las fechas ISO, la salida de sesión y
# que ninguna respuesta exponga contraseñas. Todo lo que crea lleva prefijo E2E y se borra al final,
# en el orden que exigen las claves foráneas; las cuentas usan cookies en /tmp/*.jar.
# Nunca escribe en la base `universidad`: apunte el servidor a la base de pruebas.
B=${BASE_URL:-http://localhost:8080}
ADMIN_USR=${ADMIN_USR:-e2e_admin}; ADMIN_CLAVE=${ADMIN_CLAVE:-ClaveAdmin123}
PROF_USR=${PROF_USR:-e2e_prof};   PROF_CLAVE=${PROF_CLAVE:-ClaveProfesor1}   # ficha: profesor 3
EST_USR=${EST_USR:-e2e_ana};      EST_CLAVE=${EST_CLAVE:-ClaveEstudiante1}   # ficha: estudiante 1 (carrera 3)
fallos=0
CODE=""; BODY=""

run() { # run <jar> <method> <ruta> [json]
  local jar=$1 m=$2 r=$3 body=$4 out
  if [ -n "$body" ]; then
    out=$(curl -s -b "$jar" -c "$jar" -X "$m" -H 'Content-Type: application/json' -d "$body" "$B$r" -w '\n%{http_code}')
  else
    out=$(curl -s -b "$jar" -c "$jar" -X "$m" "$B$r" -w '\n%{http_code}')
  fi
  CODE=${out##*$'\n'}
  BODY=${out%$'\n'*}
}
check() { # check <codigo esperado> <descripcion>
  if [ "$CODE" = "$1" ]; then
    printf '  ok    %-4s %s\n' "$1" "$2"
  else
    printf '  FALLA %-4s esperado, obtuve %s :: %s | %s\n' "$1" "$CODE" "$2" "${BODY:0:260}"
    fallos=$((fallos + 1))
  fi
}
asegura() { # asegura <obtenido> <esperado> <descripcion>
  if [ "$1" = "$2" ]; then
    printf '  ok    %s = %s\n' "$3" "$2"
  else
    printf '  FALLA %s: esperado %s, obtuve %s\n' "$3" "$2" "$1"
    fallos=$((fallos + 1))
  fi
}
seccion() { printf '\n=== %s\n' "$1"; }
login() { # login <jar> <usuario> <clave>
  local out
  out=$(curl -s -c "$1" -X POST -H 'Content-Type: application/json' \
    -d "{\"username\":\"$2\",\"password\":\"$3\"}" "$B/api/login" -w '\n%{http_code}')
  CODE=${out##*$'\n'}; BODY=${out%$'\n'*}
  check 200 "login $2"
}
login_negro() { # login_negro <usuario> <clave> : deja solo el codigo en CODE
  local out
  out=$(curl -s -X POST -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" "$B/api/login" -w '\n%{http_code}')
  CODE=${out##*$'\n'}; BODY=${out%$'\n'*}
}
lista() { curl -s -b "$1" "$B/api/$2"; }
# idde <claveDelId> <campo> <valor> : busca en la lista leida por stdin
idde() { python -c 'import json,sys
try: d=json.load(sys.stdin)
except Exception: d=[]
print(next((str(x[sys.argv[1]]) for x in d if str(x.get(sys.argv[2]))==sys.argv[3]),""))' "$1" "$2" "$3"; }
# dato <expresion python sobre d> [var=valor...] : d es el ultimo cuerpo de respuesta
dato() {
  local expr=$1; shift
  python -c 'import json,sys
expr=sys.argv[1]
try:
    d=json.load(sys.stdin)
except Exception:
    d=None
ns={"d":d,"any":any,"len":len,"int":int,"str":str,"sorted":sorted,"sum":sum}
for a in sys.argv[2:]:
    k,v=a.split("=",1); ns[k]=v
try:
    print(eval(expr,ns))
except Exception as ex:
    print("<sin dato: %s>" % ex)
' "$expr" "$@" <<< "$BODY"
}
# ids_de <clave> : todos los valores de esa clave en la lista leida por stdin
ids_de() { python -c 'import json,sys
try: d=json.load(sys.stdin)
except Exception: d=[]
print(" ".join(str(x[sys.argv[1]]) for x in d if x.get(sys.argv[1]) is not None))' "$1"; }
# starts_with <lista-stdin> <claveId> <campo> <prefijo> : ids de lo que empiece por el prefijo
starts_with() { python -c 'import json,sys
try: d=json.load(sys.stdin)
except Exception: d=[]
print(" ".join(str(x[sys.argv[1]]) for x in d if str(x.get(sys.argv[2],"")).startswith(sys.argv[3])))' "$1" "$2" "$3"; }
# cuantos <campo-stdin> <prefijo> : cuántas filas de la lista leida por stdin empiezan por el prefijo
cuantos() { python -c 'import json,sys
try: d=json.load(sys.stdin)
except Exception: d=[]
print(len([x for x in d if str(x.get(sys.argv[1],"")).startswith(sys.argv[2])]))' "$1" "$2"; }

limpia_seccion() { # limpia_seccion <idSeccion> : borra lo que cuelga del grupo
  local s=$1 id par
  for par in "calificaciones|idCalificacion" "asistencias|idAsistencia" "matriculas|idMatricula"; do
    for id in $(lista /tmp/adm.jar "${par%%|*}?seccion=$s" | ids_de "${par##*|}"); do
      run /tmp/adm.jar DELETE "/api/${par%%|*}/$id"
    done
  done
}

# ---------------------------------------------------------------- sesión y pre-limpieza
rm -f /tmp/adm.jar /tmp/prof.jar /tmp/est.jar /tmp/anon.jar /tmp/out.jar /tmp/api.jar
touch /tmp/anon.jar

seccion "LOGIN"
login /tmp/adm.jar "$ADMIN_USR" "$ADMIN_CLAVE"
login /tmp/prof.jar "$PROF_USR" "$PROF_CLAVE"
run /tmp/adm.jar GET /api/me; check 200 "GET /api/me del administrativo"
printf '  sesion: %s\n' "$BODY"
run /tmp/anon.jar GET /api/periodos; check 401 "un anonimo NO lee el catalogo de periodos"
run /tmp/anon.jar GET /api/mi;       check 401 "un anonimo NO tiene datos propios"

seccion "PRE-LIMPIEZA de restos E2E (en el orden que exigen las claves foraneas)"
for s in $(lista /tmp/adm.jar secciones | starts_with idSeccion aula "E2E-"); do
  limpia_seccion "$s"
  run /tmp/adm.jar DELETE "/api/secciones/$s"
  printf '  pre-limpieza seccion %-4s -> %s\n' "$s" "$CODE"
done
for id in $(lista /tmp/adm.jar cursos | starts_with idCurso nombre "Curso E2E"); do
  run /tmp/adm.jar DELETE "/api/cursos/$id"; printf '  pre-limpieza curso %-4s -> %s\n' "$id" "$CODE"
done
for id in $(lista /tmp/adm.jar usuarios | starts_with idUsuario username "e2e_api_"); do
  run /tmp/adm.jar DELETE "/api/usuarios/$id"
done
for id in $(lista /tmp/adm.jar profesores | starts_with idProfesor email "e2e."); do
  run /tmp/adm.jar DELETE "/api/profesores/$id"
done
for id in $(lista /tmp/adm.jar periodos | starts_with idPeriodo nombre "E2E"); do
  run /tmp/adm.jar DELETE "/api/periodos/$id"
done
for id in $(lista /tmp/adm.jar facultades | starts_with idFacultad nombre "Facultad E2E"); do
  run /tmp/adm.jar DELETE "/api/facultades/$id"
done

# ---------------------------------------------------------------- altas
seccion "ADMIN: alta de facultad, periodos, profesor y cursos"
run /tmp/adm.jar POST /api/facultades '{"nombre":"Facultad E2E","decano":"Decano E2E"}'
check 201 "POST /api/facultades"
FAC=$(lista /tmp/adm.jar facultades | idde idFacultad nombre "Facultad E2E")

# La ventana del periodo contiene las fechas de asistencia de abajo: ninguna puede ser futura.
run /tmp/adm.jar POST /api/periodos "{\"nombre\":\"E2E Otono\",\"fechaInicio\":\"2026-09-01\",\"fechaFin\":\"2026-12-20\",\"estado\":\"activo\"}"
check 201 "POST /api/periodos (el estado en minusculas se normaliza)"
PER=$(lista /tmp/adm.jar periodos | idde idPeriodo nombre "E2E Otono")
run /tmp/adm.jar GET /api/periodos
asegura "$(dato '[p["estado"] for p in d if p["nombre"]=="E2E Otono"]')" "['ACTIVO']" "estado normalizado"
run /tmp/adm.jar POST /api/periodos '{"nombre":"E2E Cerrado","fechaInicio":"2026-02-01","fechaFin":"2026-06-30","estado":"CERRADO"}'
check 201 "POST /api/periodos cerrado"
PERX=$(lista /tmp/adm.jar periodos | idde idPeriodo nombre "E2E Cerrado")

run /tmp/adm.jar POST /api/profesores '{"nombre":"N","apellido":"A","email":"na@x.com","idFacultad":0}'
check 400 "profesor con facultad inexistente"
run /tmp/adm.jar POST /api/profesores "{\"nombre\":\"Profe\",\"apellido\":\"E2E\",\"email\":\"e2e.prueba@ut.edu.co\",\"idFacultad\":$FAC}"
check 201 "POST /api/profesores"
PROF=$(lista /tmp/adm.jar profesores | idde idProfesor email "e2e.prueba@ut.edu.co")

alta_curso() { # alta_curso <nombre> <creditos> <idCarrera> : imprime el id creado
  run /tmp/adm.jar POST /api/cursos "{\"nombre\":\"$1\",\"creditos\":$2,\"idCarrera\":$3,\"idProfesor\":$PROF}"
  check 201 "curso $1" 1>&2
  lista /tmp/adm.jar cursos | idde idCurso nombre "$1"
}
CUR=$(alta_curso "Curso E2E" 3 3)
CUR2=$(alta_curso "Curso E2E Lleno" 3 3)
CUR3=$(alta_curso "Curso E2E Ajeno" 3 1)
CUR4=$(alta_curso "Curso E2E Cerrado" 3 3)
CUR5=$(alta_curso "Curso E2E Creditos" 10 3)
CUR6=$(alta_curso "Curso E2E Creditos 2" 10 3)

alta_seccion() { # alta_seccion <aula> <curso> <profesor> <periodo> <cupo> : imprime el id creado
  run /tmp/adm.jar POST /api/secciones "{\"idCurso\":$2,\"idProfesor\":$3,\"idPeriodo\":$4,\"aula\":\"$1\",\"cupoMaximo\":$5}"
  check 201 "seccion $1" 1>&2
  lista /tmp/adm.jar secciones | idde idSeccion aula "$1"
}
# Nelson (profesor 3) dicta las secciones propias; la 4 (Adriana) es ajena para el login de prueba.
SA=$(alta_seccion "E2E-A" "$CUR"  3 "$PER"  30)
SB=$(alta_seccion "E2E-B" "$CUR"  4 "$PER"  25)
SC=$(alta_seccion "E2E-C" "$CUR2" 3 "$PER"   1)
SD=$(alta_seccion "E2E-D" "$CUR3" 3 "$PER"  20)
SE=$(alta_seccion "E2E-E" "$CUR4" 3 "$PERX" 20)
SF=$(alta_seccion "E2E-F" "$CUR5" 3 "$PER"  20)
SG=$(alta_seccion "E2E-G" "$CUR6" 3 "$PER"  20)
run /tmp/adm.jar POST /api/secciones "{\"idCurso\":$CUR,\"idProfesor\":3,\"idPeriodo\":$PER,\"aula\":\"E2E-A\",\"cupoMaximo\":30}"
check 409 "seccion duplicada (curso + aula + periodo)"
run /tmp/adm.jar GET "/api/secciones/$SA"; check 200 "GET /api/secciones/{id}"
run /tmp/adm.jar GET /api/secciones/999999; check 404 "GET seccion inexistente"
run /tmp/adm.jar GET "/api/secciones/$SA"
asegura "$(dato 'd["cuposOcupados"]')" "0" "ocupacion inicial de E2E-A"

ANA=1; DAVID=2; SARA=3; LAURA=5
printf '  datos E2E -> facultad=%s periodos=%s/%s profesor=%s cursos=%s..%s secciones=%s..%s\n' \
  "$FAC" "$PER" "$PERX" "$PROF" "$CUR" "$CUR6" "$SA" "$SG"

seccion "MATRICULA: el curso y el periodo los decide la seccion"
run /tmp/adm.jar POST /api/matriculas "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"nota\":null}"
check 201 "POST /api/matriculas (solo estudiante + seccion)"
printf '  derivado por el servidor: idCurso=%s periodo=%s estado=%s\n' \
  "$(dato 'd["idCurso"]')" "$(dato 'd["periodo"]')" "$(dato 'd["estado"]')"
asegura "$(dato 'str(d["idCurso"])')" "$CUR" "curso derivado de la seccion"
asegura "$(dato 'd["periodo"]')" "E2E Otono" "periodo derivado de la seccion"
MAT=$(lista /tmp/adm.jar "matriculas?estudiante=$ANA&seccion=$SA" | idde idMatricula idSeccion "$SA")
run /tmp/adm.jar POST /api/matriculas "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"nota\":null}"
check 409 "matricula repetida en la misma seccion"
run /tmp/adm.jar POST /api/matriculas "{\"idEstudiante\":$ANA,\"idSeccion\":$SE,\"nota\":null}"
check 409 "matricula en un periodo CERRADO"
run /tmp/adm.jar POST /api/matriculas "{\"idEstudiante\":$ANA,\"idSeccion\":$SD,\"nota\":null}"
check 409 "matricula en un curso de otra carrera"
run /tmp/adm.jar POST /api/matriculas "{\"idEstudiante\":$ANA,\"idSeccion\":$SB,\"nota\":null}"
check 409 "otro grupo del mismo curso y periodo (uq_matricula)"
printf '  mensaje: %s\n' "$BODY"
run /tmp/adm.jar POST /api/matriculas "{\"idEstudiante\":$ANA,\"idSeccion\":999999,\"nota\":null}"
check 400 "matricula en una seccion inexistente"
run /tmp/adm.jar POST /api/matriculas "{\"idEstudiante\":$ANA,\"idSeccion\":$SF,\"nota\":null}"
check 201 "matricula de 10 creditos (Ana va por 13)"
run /tmp/adm.jar POST /api/matriculas "{\"idEstudiante\":$ANA,\"idSeccion\":$SG,\"nota\":null}"
check 409 "tope de 20 creditos por periodo"
run /tmp/adm.jar POST /api/matriculas "{\"idEstudiante\":$DAVID,\"idSeccion\":$SC,\"nota\":null}"
check 201 "primera matricula en la seccion de cupo 1"
run /tmp/adm.jar POST /api/matriculas "{\"idEstudiante\":$ANA,\"idSeccion\":$SC,\"nota\":null}"
check 409 "la seccion de cupo 1 ya esta llena"
run /tmp/adm.jar POST /api/matriculas "{\"idEstudiante\":$DAVID,\"idSeccion\":$SA,\"nota\":null}"
check 201 "David tambien en E2E-A (regla de asistencia)"

seccion "CUPOS: lo que el formulario no debe adivinar"
run /tmp/adm.jar GET "/api/secciones/$SC/cupos"; check 200 "GET /api/secciones/{id}/cupos"
printf '  E2E-C: %s\n' "$BODY"
asegura "$(dato 'd["disponibles"]')" "0" "cupos libres de E2E-C"
run /tmp/adm.jar GET "/api/secciones/$SG/cupos"; check 200 "GET cupos de una seccion vacia"
asegura "$(dato 'd["disponibles"]')" "20" "cupos libres de E2E-G"
run /tmp/adm.jar GET /api/secciones/999999/cupos; check 404 "cupos de una seccion inexistente"
run /tmp/prof.jar GET "/api/secciones/$SB/cupos"; check 403 "el profesor NO ve los cupos de una seccion ajena"
run /tmp/adm.jar GET "/api/estudiantes/$SARA/secciones-inscribibles"; check 200 "secciones inscribibles de Sara"
printf '  inscribibles de Sara: %s\n' "$(dato '[s["aula"] for s in d if str(s["aula"]).startswith("E2E-")]')"
asegura "$(dato 'any(s["aula"]=="E2E-D" for s in d)')" "True" "E2E-D es inscribible para su carrera"
run /tmp/adm.jar GET "/api/estudiantes/$ANA/secciones-inscribibles"; check 200 "secciones inscribibles de Ana"
printf '  inscribibles de Ana: %s\n' "$(dato '[s["aula"] for s in d if str(s["aula"]).startswith("E2E-")]')"
asegura "$(dato 'any(s["aula"]=="E2E-D" for s in d)')" "False" "E2E-D (otra carrera) NO aparece para Ana"
asegura "$(dato 'any(str(s["idSeccion"])==SA for s in d)' "SA=$SA")" "False" "Ana no ve la seccion donde ya esta matriculada"
asegura "$(dato 'any(s["aula"]=="E2E-E" for s in d)')" "False" "la seccion de periodo cerrado no es inscribible"
asegura "$(dato 'any(s["aula"]=="E2E-B" for s in d)')" "False" "E2E-B (otro grupo del mismo curso) no se ofrece"
asegura "$(dato 'any(s["aula"]=="E2E-C" for s in d)')" "False" "E2E-C (llena) no se ofrece"
asegura "$(dato 'any(s["aula"]=="E2E-G" for s in d)')" "False" "E2E-G (pasaria de los 20 creditos) no se ofrece"
run /tmp/prof.jar GET "/api/estudiantes/$ANA/secciones-inscribibles"; check 403 "el profesor NO consulta inscribibles ajenos"

seccion "ASISTENCIA: solo matriculados y solo dentro del periodo"
run /tmp/prof.jar POST /api/asistencias "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"fecha\":\"2026-10-05\",\"estado\":\"ASISTIO\"}"
check 201 "asistencia en seccion propia"
ASI=$(lista /tmp/prof.jar "asistencias?seccion=$SA&fecha=2026-10-05" | idde idAsistencia idEstudiante "$ANA")
run /tmp/prof.jar POST /api/asistencias "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"fecha\":\"2026-10-05\",\"estado\":\"FALTO\"}"
check 409 "no se repite la asistencia del mismo dia"
run /tmp/prof.jar POST /api/asistencias "{\"idEstudiante\":$ANA,\"idSeccion\":$SB,\"fecha\":\"2026-10-05\",\"estado\":\"ASISTIO\"}"
check 403 "asistencia en seccion ajena"
run /tmp/prof.jar PUT "/api/asistencias/$ASI" "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"fecha\":\"2026-10-05\",\"estado\":\"TARDANZA\"}"
check 200 "corrige su propia asistencia"
run /tmp/prof.jar POST /api/asistencias "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"fecha\":\"2026-08-15\",\"estado\":\"ASISTIO\"}"
check 409 "fecha fuera de la ventana del periodo"
run /tmp/prof.jar POST /api/asistencias "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"fecha\":\"2026-11-30\",\"estado\":\"ASISTIO\"}"
check 400 "fecha de asistencia futura"
run /tmp/prof.jar POST /api/asistencias "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"fecha\":\"no-fecha\",\"estado\":\"ASISTIO\"}"
check 400 "asistencia con fecha invalida"
run /tmp/prof.jar POST /api/asistencias "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"fecha\":\"2026-10-06\",\"estado\":\"VAGANDO\"}"
check 400 "asistencia con estado invalido"
run /tmp/adm.jar POST /api/asistencias "{\"idEstudiante\":$LAURA,\"idSeccion\":$SA,\"fecha\":\"2026-10-06\",\"estado\":\"ASISTIO\"}"
check 409 "no se pasa lista a quien no esta matriculado"
run /tmp/prof.jar GET "/api/asistencias?seccion=$SB"; check 403 "el profesor NO consulta la seccion ajena"
for d in 09 12 14; do
  run /tmp/prof.jar POST /api/asistencias "{\"idEstudiante\":$DAVID,\"idSeccion\":$SA,\"fecha\":\"2026-10-$d\",\"estado\":\"ASISTIO\"}"
  check 201 "control de David el 2026-10-$d"
done
run /tmp/prof.jar POST /api/asistencias "{\"idEstudiante\":$DAVID,\"idSeccion\":$SA,\"fecha\":\"2026-10-13\",\"estado\":\"FALTO\"}"
check 201 "David acumula 1 falta en 4 controles (justo el 75 %)"

seccion "CALIFICACIONES: 30 % + 30 % + 40 % y la asistencia manda sobre la nota"
run /tmp/prof.jar POST /api/calificaciones "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"notaParcial1\":3.5,\"notaParcial2\":4.0,\"notaExamenFinal\":null}"
check 201 "calificacion con examen pendiente"
printf '  respuesta: %s\n' "$BODY"
asegura "$(dato 'd["estado"]')" "EN_CURSO" "estado con el examen sin cargar"
CAL=$(lista /tmp/prof.jar "calificaciones?seccion=$SA" | idde idCalificacion idEstudiante "$ANA")
run /tmp/adm.jar GET "/api/matriculas?estudiante=$ANA&seccion=$SA"
asegura "$(dato 'str(d[0]["nota"])')" "None" "la definitiva no se propaga con el examen pendiente"
asegura "$(dato 'd[0]["estado"]')" "EN_CURSO" "matricula todavia en curso"
run /tmp/prof.jar POST /api/calificaciones "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"notaParcial1\":3.5,\"notaParcial2\":4.0,\"notaExamenFinal\":4.0}"
check 409 "una sola calificacion por estudiante y seccion"
run /tmp/prof.jar POST /api/calificaciones "{\"idEstudiante\":$DAVID,\"idSeccion\":$SB,\"notaParcial1\":3.5,\"notaParcial2\":4.0,\"notaExamenFinal\":4.0}"
check 403 "calificacion en seccion ajena"
run /tmp/adm.jar POST /api/calificaciones "{\"idEstudiante\":$DAVID,\"idSeccion\":$SB,\"notaParcial1\":3.5,\"notaParcial2\":4.0,\"notaExamenFinal\":4.0}"
check 409 "no se califica al que no esta matriculado en la seccion"
run /tmp/prof.jar PUT "/api/calificaciones/$CAL" "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"notaParcial1\":5.0,\"notaParcial2\":4.5,\"notaExamenFinal\":9.9}"
check 400 "nota fuera de la escala 0.0-5.0"
run /tmp/prof.jar PUT "/api/calificaciones/$CAL" "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"notaParcial1\":3.5,\"notaParcial2\":4.0,\"notaExamenFinal\":4.0,\"estado\":\"APROBADO\",\"promedioFinal\":5.0}"
check 200 "el cliente intenta forzar estado y promedio"
printf '  recalculado por el servidor: %s\n' "$BODY"
asegura "$(dato 'd["promedioFinal"]')" "3.85" "promedio 30/30/40"
asegura "$(dato 'd["estado"]')" "APROBADO" "estado derivado de la nota"
run /tmp/adm.jar GET "/api/matriculas?estudiante=$ANA&seccion=$SA"
printf '  matricula de Ana: nota %s | estado %s\n' "$(dato 'd[0]["nota"]')" "$(dato 'd[0]["estado"]')"
asegura "$(dato 'str(d[0]["nota"])')" "3.85" "nota definitiva propagada a la matricula"
asegura "$(dato 'd[0]["estado"]')" "APROBADO" "estado de la matricula sincronizado"
run /tmp/prof.jar POST /api/calificaciones "{\"idEstudiante\":$DAVID,\"idSeccion\":$SA,\"notaParcial1\":3.5,\"notaParcial2\":4.0,\"notaExamenFinal\":4.0}"
check 201 "David cierra notas con 75 % exacto de asistencia"
printf '  respuesta: %s\n' "$BODY"
asegura "$(dato 'd["estado"]')" "APROBADO" "el limite del 75 % no pierde el curso"
for d in 06 07 08; do
  run /tmp/prof.jar POST /api/asistencias "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"fecha\":\"2026-10-$d\",\"estado\":\"FALTO\"}"
  check 201 "falta de Ana el 2026-10-$d"
done
run /tmp/prof.jar PUT "/api/calificaciones/$CAL" "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"notaParcial1\":3.5,\"notaParcial2\":4.0,\"notaExamenFinal\":4.0}"
check 200 "recalcula la calificacion con 3 faltas de 4 controles"
printf '  calificacion: %s\n' "$BODY"
asegura "$(dato 'd["estado"]')" "PERDIDO_POR_FALTA_DE_ASISTENCIA" "la inasistencia pesa mas que la nota"
run /tmp/adm.jar GET "/api/matriculas?estudiante=$ANA&seccion=$SA"
printf '  matricula de Ana: nota %s | estado %s\n' "$(dato 'd[0]["nota"]')" "$(dato 'd[0]["estado"]')"
asegura "$(dato 'd[0]["estado"]')" "PERDIDO_POR_FALTA_DE_ASISTENCIA" "la matricula tambien pierde por asistencia"
run /tmp/prof.jar POST /api/calificaciones "{\"idEstudiante\":999999,\"idSeccion\":$SA,\"notaParcial1\":3.0,\"notaParcial2\":3.0,\"notaExamenFinal\":3.0}"
check 400 "calificacion con estudiante inexistente"

seccion "BORRADO PROTEGIDO"
run /tmp/adm.jar DELETE "/api/secciones/$SA"; check 409 "una seccion con matriculados no se borra"
run /tmp/adm.jar DELETE "/api/cursos/$CUR";  check 409 "un curso con matriculados no se borra (clave foranea)"
run /tmp/adm.jar DELETE "/api/secciones/$SG"; check 200 "una seccion vacia si se borra"

seccion "VALIDACIONES DE FORMA"
run /tmp/adm.jar POST /api/facultades '{"nombre":""}'; check 400 "facultad sin nombre"
run /tmp/adm.jar POST /api/profesores '{"nombre":"X","apellido":"Y","email":"correo-malo","idFacultad":1}'; check 400 "profesor con email invalido"
run /tmp/adm.jar POST /api/cursos '{"nombre":"Curso E2e Malo","creditos":0,"idCarrera":1,"idProfesor":1}'; check 400 "curso con creditos 0"
run /tmp/adm.jar POST /api/cursos '{"nombre":"Curso E2e Malo 2","creditos":11,"idCarrera":1,"idProfesor":1}'; check 400 "curso con creditos fuera de 1-10"
run /tmp/adm.jar POST /api/periodos '{"nombre":"E2E Malas fechas","fechaInicio":"2026-06-01","fechaFin":"2026-01-01","estado":"ACTIVO"}'; check 400 "periodo con fechaFin anterior a fechaInicio"
run /tmp/adm.jar POST /api/periodos '{"nombre":"E2E Sin estado","fechaInicio":"2026-01-01","fechaFin":"2026-06-01","estado":"APERTURADO"}'; check 400 "periodo con estado desconocido"
run /tmp/adm.jar POST /api/secciones "{\"idCurso\":$CUR2,\"idProfesor\":3,\"idPeriodo\":$PER,\"aula\":\"E2E-Z\",\"cupoMaximo\":0}"; check 400 "seccion con cupo 0"
run /tmp/adm.jar POST /api/secciones "{\"idCurso\":$CUR2,\"idProfesor\":3,\"idPeriodo\":$PER,\"aula\":\"E2E-Z\",\"cupoMaximo\":201}"; check 400 "seccion con cupo 201"
run /tmp/adm.jar POST /api/matriculas "{\"idEstudiante\":$ANA,\"idSeccion\":$SF,\"nota\":9.9}"; check 400 "matricula con nota fuera de 0-5"
run /tmp/adm.jar POST /api/profesores '{"nombre":"N","apellido":"A","email":"na@x.com","idFacultad":999999}'; check 400 "referencia a facultad inexistente"
run /tmp/adm.jar POST /api/asistencias "{\"idEstudiante\":$ANA,\"idSeccion\":999999,\"fecha\":\"2026-10-16\",\"estado\":\"ASISTIO\"}"; check 400 "asistencia con seccion inexistente"
run /tmp/adm.jar POST /api/secciones "{\"idCurso\":$CUR2,\"idProfesor\":1,\"idPeriodo\":999999,\"aula\":\"E2E-Y\",\"cupoMaximo\":10}"; check 400 "seccion con periodo inexistente"
run /tmp/adm.jar PUT /api/matriculas/999999 "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"nota\":null}"; check 404 "actualizar una matricula inexistente"

seccion "FILTROS POR QUERY STRING"
run /tmp/adm.jar GET "/api/cursos?profesor=$PROF"; check 200 "cursos filtrados por profesor"
printf '  cursos del profesor E2E: %s\n' "$(dato 'len(d)')"
run /tmp/adm.jar GET "/api/secciones?periodo=$PER"; check 200 "secciones filtradas por periodo"
printf '  secciones del periodo E2E: %s\n' "$(dato 'len(d)')"
run /tmp/adm.jar GET "/api/secciones?curso=$CUR"; check 200 "secciones filtradas por curso"
run /tmp/adm.jar GET "/api/matriculas?estudiante=$ANA"; check 200 "matriculas filtradas por estudiante"
printf '  matriculas de Ana: %s\n' "$(dato 'len(d)')"
run /tmp/adm.jar GET "/api/matriculas?seccion=$SA"; check 200 "matriculas filtradas por seccion"
run /tmp/adm.jar GET "/api/profesores?facultad=abc"; check 400 "filtro no numerico"
run /tmp/adm.jar GET "/api/asistencias?seccion=$SA&fecha=16-10-2026"; check 400 "filtro de fecha invalido"
run /tmp/adm.jar GET "/api/periodos?estado=activo"; check 200 "periodos filtrados por estado"
printf '  periodos activos: %s\n' "$(dato 'len(d)')"

seccion "CUENTAS DE ACCESO"
run /tmp/adm.jar POST /api/usuarios "{\"username\":\"e2e_api_profe\",\"password\":\"ClaveSegura123\",\"rol\":\"PROFESOR\",\"idProfesor\":$PROF,\"estado\":1}"
check 201 "POST /api/usuarios con ficha de profesor"
USR=$(lista /tmp/adm.jar usuarios | idde idUsuario username "e2e_api_profe")
run /tmp/adm.jar POST /api/usuarios "{\"username\":\"e2e_api_profe\",\"password\":\"ClaveSegura123\",\"rol\":\"PROFESOR\",\"idProfesor\":$PROF,\"estado\":1}"
check 409 "usuario duplicado"
run /tmp/adm.jar POST /api/usuarios '{"username":"ab","password":"ClaveSegura123","rol":"ADMINISTRATIVO","estado":1}'; check 400 "username muy corto"
run /tmp/adm.jar POST /api/usuarios '{"username":"e2e_api_corto","password":"123","rol":"ADMINISTRATIVO","estado":1}'; check 400 "clave menor a 10 caracteres"
run /tmp/adm.jar POST /api/usuarios '{"username":"e2e_api_rol","password":"ClaveSegura123","rol":"DIRECTIVO","estado":1}'; check 400 "rol inexistente"
run /tmp/adm.jar POST /api/usuarios '{"username":"e2e_api_sin_id","password":"ClaveSegura123","rol":"PROFESOR","estado":1}'; check 400 "profesor sin ficha vinculada"
run /tmp/adm.jar POST /api/usuarios "{\"username\":\"e2e_api_est\",\"password\":\"ClaveSegura123\",\"rol\":\"ESTUDIANTE\",\"idEstudiante\":999999,\"estado\":1}"; check 400 "ficha de estudiante inexistente"
run /tmp/adm.jar POST /api/usuarios '{"username":"e2e_api_est","password":"ClaveSegura123","rol":"ESTUDIANTE","idEstudiante":2,"estado":1}'
check 201 "POST /api/usuarios de estudiante"
USR2=$(lista /tmp/adm.jar usuarios | idde idUsuario username "e2e_api_est")
login /tmp/api.jar e2e_api_profe ClaveSegura123
run /tmp/api.jar POST /api/periodos '{"nombre":"E2E Prohibido","fechaInicio":"2026-01-01","fechaFin":"2026-02-01","estado":"ACTIVO"}'; check 403 "la cuenta recien creada no crea catalogos"
run /tmp/adm.jar GET /api/usuarios; check 200 "GET /api/usuarios"
if [[ "$BODY" == *[Pp]assword* ]]; then
  printf '  FALLA la lista de cuentas expuso el campo password\n'; fallos=$((fallos + 1))
else
  printf '  ok    la lista de cuentas NO incluye el campo password\n'
fi
if [[ "$BODY" == *ClaveSegura123* ]]; then
  printf '  FALLA la lista de cuentas expuso una clave en texto plano\n'; fallos=$((fallos + 1))
fi
run /tmp/adm.jar PUT "/api/usuarios/$USR2" '{"username":"e2e_api_est","rol":"ESTUDIANTE","idEstudiante":2,"estado":0}'; check 200 "desactivar una cuenta"
login_negro e2e_api_est ClaveSegura123; check 401 "la cuenta desactivada no inicia sesion"
run /tmp/adm.jar PUT "/api/usuarios/$USR2" '{"username":"e2e_api_est","password":"ClaveNueva12345","rol":"ESTUDIANTE","idEstudiante":2,"estado":1}'; check 200 "reactivar y cambiar la clave"
login_negro e2e_api_est ClaveSegura123;  check 401 "la clave antigua dejo de servir"
login_negro e2e_api_est ClaveNueva12345; check 200 "la clave nueva funciona"
MIO=$(lista /tmp/adm.jar usuarios | idde idUsuario username "$ADMIN_USR")
run /tmp/adm.jar PUT "/api/usuarios/$MIO" "{\"username\":\"$ADMIN_USR\",\"rol\":\"ADMINISTRATIVO\",\"estado\":0}"; check 400 "no puedo desactivarme a mi mismo"
run /tmp/adm.jar DELETE "/api/usuarios/$MIO"; check 400 "no puedo eliminarme a mi mismo"
run /tmp/adm.jar DELETE /api/usuarios/999999; check 404 "eliminar cuenta inexistente"

seccion "PROFESOR: lectura y permisos"
run /tmp/prof.jar GET /api/periodos; check 200 "el profesor lee catalogos"
run /tmp/prof.jar GET /api/usuarios; check 403 "el profesor NO lee cuentas"
run /tmp/prof.jar GET /api/estudiantes; check 200 "el profesor lee el listado de estudiantes (pasa lista)"
run /tmp/prof.jar POST /api/periodos '{"nombre":"E2E-X","fechaInicio":"2026-01-01","fechaFin":"2026-02-01","estado":"ACTIVO"}'; check 403 "el profesor NO crea periodos"
run /tmp/prof.jar POST /api/cursos '{"nombre":"Curso E2e Y","creditos":3,"idCarrera":1,"idProfesor":1}'; check 403 "el profesor NO crea cursos"
run /tmp/prof.jar DELETE "/api/matriculas/$MAT"; check 403 "el profesor NO elimina matriculas"
run /tmp/prof.jar GET /api/asistencias; check 200 "el profesor lee asistencias de sus secciones"
printf '  asistencias visibles: %s\n' "$(dato 'len(d)')"
run /tmp/prof.jar GET /api/calificaciones; check 200 "el profesor lee calificaciones de sus secciones"
run /tmp/prof.jar GET /api/secciones; check 200 "el profesor solo ve sus secciones"
printf '  sus aulas E2E: %s\n' "$(dato 'sorted(s["aula"] for s in d if str(s["aula"]).startswith("E2E-"))')"
run /tmp/prof.jar GET /api/mi; check 200 "GET /api/mi del profesor"
printf '  perfil: %s\n' "$BODY"
run /tmp/prof.jar GET /api/mi/secciones; check 200 "GET /api/mi/secciones"
printf '  ocupacion informada: %s\n' "$(dato '[s["ocupacion"] for s in d if s["aula"]=="E2E-A"]')"
run /tmp/prof.jar GET /api/mi/asistencias; check 200 "GET /api/mi/asistencias"
run /tmp/prof.jar GET /api/mi/matriculas; check 403 "las matriculas propias solo aplican al estudiante"

seccion "ESTUDIANTE: solo lo suyo"
login /tmp/est.jar "$EST_USR" "$EST_CLAVE"
for r in /api/estudiantes /api/matriculas /api/usuarios /api/asistencias /api/calificaciones /api/profesores /api/facultades /api/secciones /api/carreras; do
  run /tmp/est.jar GET "$r"; check 403 "el estudiante NO lee $r"
done
run /tmp/est.jar POST /api/asistencias "{\"idEstudiante\":$ANA,\"idSeccion\":$SA,\"fecha\":\"2026-10-19\",\"estado\":\"ASISTIO\"}"; check 403 "el estudiante NO escribe asistencias"
run /tmp/est.jar DELETE "/api/matriculas/$MAT"; check 403 "el estudiante NO elimina"
run /tmp/est.jar GET "/api/secciones/$SA/cupos"; check 403 "el estudiante NO consulta cupos"
for r in /api/mi /api/mi/secciones /api/mi/calificaciones /api/mi/asistencias /api/mi/matriculas; do
  run /tmp/est.jar GET "$r"; check 200 "el estudiante lee $r"
done
run /tmp/est.jar GET /api/mi; check 200 "perfil propio"
printf '  materias: %s | promedio %s | asistencia %s%%\n' \
  "$(dato 'd["materias"]')" "$(dato 'd["promedioGeneral"]')" "$(dato 'd["porcentajeAsistencia"]')"
run /tmp/est.jar GET /api/mi/matriculas; check 200 "matriculas propias"
printf '  sus E2E: %s\n' "$(dato '[(m["curso"],m["estado"]) for m in d if m["periodo"]=="E2E Otono"]')"

seccion "SALIDA Y FORMATOS"
run /tmp/adm.jar GET /api/periodos; check 200 "periodos con fechas ISO"
printf '  fechas: %s .. %s\n' "$(dato 'd[0]["fechaInicio"]')" "$(dato 'd[0]["fechaFin"]')"
asegura "$(dato 'len(str(d[0]["fechaInicio"]))')" "10" "formato de fecha AAAA-MM-DD"
run /tmp/out.jar POST /api/login "{\"username\":\"$ADMIN_USR\",\"password\":\"$ADMIN_CLAVE\"}"; check 200 "login para probar el logout"
run /tmp/out.jar POST /api/logout; check 204 "POST /api/logout"
run /tmp/out.jar GET /api/me; check 401 "tras el logout la sesion ya no sirve"
run /tmp/anon.jar GET /api/health; check 200 "GET /api/health"

seccion "LOGIN FALLIDO"
login_negro "$ADMIN_USR" clave-malisima-1;  check 401 "login con clave erronea"
login_negro no-existe-xx ClaveSegura123;    check 401 "login de usuario inexistente"
login_negro "$ADMIN_USR" "";                check 400 "login sin clave"

seccion "PAGINAS (el frente lo sirve la propia aplicacion)"
for p in login.html menu.html mi.html estudiantes.html carreras.html facultades.html profesores.html periodos.html cursos.html secciones.html matriculas.html asistencias.html calificaciones.html usuarios.html modulos.js marca.css; do
  a=$(curl -s -o /dev/null -w '%{http_code}' "$B/$p")
  ad=$(curl -s -o /dev/null -w '%{http_code}' -b /tmp/adm.jar "$B/$p")
  pr=$(curl -s -o /dev/null -w '%{http_code}' -b /tmp/prof.jar "$B/$p")
  es=$(curl -s -o /dev/null -w '%{http_code}' -b /tmp/est.jar "$B/$p")
  printf '  %-22s anon=%s admin=%s prof=%s est=%s\n' "$p" "$a" "$ad" "$pr" "$es"
done

seccion "LIMPIEZA de datos E2E (borrado en el orden que exigen las claves foraneas)"
# Se recorre lo que realmente quedó en la base, no las variables: SG ya se borró arriba y
# puede quedar algún grupo adicional (p. ej. el de los créditos) con matrícula viva.
for s in $(lista /tmp/adm.jar secciones | starts_with idSeccion aula "E2E-"); do
  limpia_seccion "$s"
  run /tmp/adm.jar DELETE "/api/secciones/$s"; check 200 "elimina la seccion $s"
done
for c in $(lista /tmp/adm.jar cursos | starts_with idCurso nombre "Curso E2E"); do
  run /tmp/adm.jar DELETE "/api/cursos/$c"; check 200 "elimina el curso $c"
done
for u in $(lista /tmp/adm.jar usuarios | starts_with idUsuario username "e2e_api_"); do
  run /tmp/adm.jar DELETE "/api/usuarios/$u"; check 200 "elimina la cuenta $u"
done
run /tmp/adm.jar DELETE "/api/profesores/$PROF"; check 200 "elimina el profesor E2E"
run /tmp/adm.jar DELETE "/api/periodos/$PER";    check 200 "elimina el periodo E2E Otono"
run /tmp/adm.jar DELETE "/api/periodos/$PERX";   check 200 "elimina el periodo E2E Cerrado"
run /tmp/adm.jar DELETE "/api/facultades/$FAC";  check 200 "elimina la facultad E2E"

asegura "$(lista /tmp/adm.jar secciones  | cuantos aula "E2E-")"      "0" "restos E2E en secciones"
asegura "$(lista /tmp/adm.jar cursos     | cuantos nombre "Curso E2E")" "0" "restos E2E en cursos"
asegura "$(lista /tmp/adm.jar usuarios   | cuantos username "e2e_api_")" "0" "restos E2E en cuentas"
asegura "$(lista /tmp/adm.jar periodos   | cuantos nombre "E2E")"       "0" "restos E2E en periodos"
asegura "$(lista /tmp/adm.jar facultades | cuantos nombre "Facultad E2E")" "0" "restos E2E en facultades"

printf '\n========================================\n'
if [ "$fallos" = 0 ]; then echo "TODO OK"; else echo "$fallos VERIFICACIONES FALLARON"; exit 1; fi
